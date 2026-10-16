package com.djk.domain.keyword.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.djk.domain.ranking.entity.RankingType;
import com.djk.support.MySqlTestContainer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(MySqlTestContainer.class)
class KeywordSearchApiTest {

    private static final String URL = "/api/v1/keywords/search";
    private static final LocalDateTime LATEST = LocalDateTime.of(2026, 7, 24, 9, 0);
    private static final LocalDateTime PREVIOUS = LATEST.minusDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        // KEYWORD 를 지우면 FK ON DELETE CASCADE 로 스냅샷·뱃지·연관어·이력·카테고리 매핑도 지워진다.
        jdbcTemplate.update("DELETE FROM KEYWORD");
        jdbcTemplate.update("DELETE FROM CATEGORY");
    }

    @Test
    @DisplayName("파라미터 없이 호출하면 최신 TREND·POTENTIAL·RISING 스냅샷의 키워드를 트렌드 지수순으로, 지수가 없으면 맨 뒤로 내려준다")
    void searchesLatestSnapshotKeywordsSortedByTrendScore() throws Exception {
        long both = insertKeyword("트렌드와포텐셜");
        long trendOnly = insertKeyword("트렌드만");
        long potentialOnly = insertKeyword("포텐셜만");
        long risingOnly = insertKeyword("라이징만");
        long previousOnly = insertKeyword("어제만");
        insertKeyword("랭킹밖");
        insertSnapshot(RankingType.TREND, both, 1, "0.9000", LATEST);
        insertSnapshot(RankingType.POTENTIAL, both, 2, "0.5000", LATEST);
        insertSnapshot(RankingType.TREND, trendOnly, 2, "0.8234", LATEST);
        insertSnapshot(RankingType.POTENTIAL, potentialOnly, 1, "0.7000", LATEST);
        insertSnapshot(RankingType.RISING, risingOnly, 1, "0.4000", LATEST);
        insertSnapshot(RankingType.TREND, previousOnly, 1, "0.9900", PREVIOUS);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("검색 결과를 조회하였습니다."))
                .andExpect(jsonPath("$.details.snapshotAt").value("2026-07-24T09:00:00"))
                .andExpect(jsonPath("$.details.page").value(0))
                .andExpect(jsonPath("$.details.size").value(20))
                .andExpect(jsonPath("$.details.totalElements").value(4))
                .andExpect(jsonPath("$.details.totalPages").value(1))
                .andExpect(jsonPath("$.details.items.length()").value(4))
                .andExpect(jsonPath("$.details.items[0].keywordId").value(both))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("트렌드와포텐셜"))
                .andExpect(jsonPath("$.details.items[0].trend.rankPosition").value(1))
                .andExpect(jsonPath("$.details.items[0].trend.trendScore").value(90.0))
                .andExpect(jsonPath("$.details.items[0].potential.rankPosition").value(2))
                .andExpect(jsonPath("$.details.items[0].potential.score").value(50.0))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("트렌드만"))
                .andExpect(jsonPath("$.details.items[1].trend.trendScore").value(82.34))
                .andExpect(jsonPath("$.details.items[1].potential").value(nullValue()))
                .andExpect(jsonPath("$.details.items[2].keywordName").value("포텐셜만"))
                .andExpect(jsonPath("$.details.items[2].trend").value(nullValue()))
                .andExpect(jsonPath("$.details.items[3].keywordName").value("라이징만"));
    }

    @Test
    @DisplayName("스냅샷이 하나도 없으면 200 과 빈 목록, snapshotAt=null 을 내려준다")
    void returnsEmptyWhenNoSnapshot() throws Exception {
        insertKeyword("랭킹밖");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.details.snapshotAt").value(nullValue()))
                .andExpect(jsonPath("$.details.totalElements").value(0))
                .andExpect(jsonPath("$.details.totalPages").value(0))
                .andExpect(jsonPath("$.details.items").isEmpty());
    }

    @Test
    @DisplayName("page 와 size 로 잘라서 내려주고 전체 개수와 페이지 수를 함께 준다")
    void paginates() throws Exception {
        for (int rank = 1; rank <= 5; rank++) {
            insertSnapshot(RankingType.TREND, insertKeyword("키워드" + rank), rank, "0.9" + rank + "00", LATEST);
        }

        // 지수가 높은 순: 키워드5(0.95) > 키워드4 > ... > 키워드1(0.91)
        mockMvc.perform(get(URL).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.page").value(1))
                .andExpect(jsonPath("$.details.size").value(2))
                .andExpect(jsonPath("$.details.totalElements").value(5))
                .andExpect(jsonPath("$.details.totalPages").value(3))
                .andExpect(jsonPath("$.details.items.length()").value(2))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("키워드3"))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("키워드2"));
    }

    @Test
    @DisplayName("q 는 앞뒤 공백을 무시하고, 이름에 q 가 들어간 키워드와 이름이 q 와 같은 키워드의 연관어를 찾는다")
    void searchesByNameAndRelatedKeywordsOfExactMatch() throws Exception {
        long malatang = insertKeyword("마라탕");
        long malaxiangguo = insertKeyword("마라샹궈");
        long hotpot = insertKeyword("훠궈");
        long delivery = insertKeyword("배달");
        long chicken = insertKeyword("치킨");
        insertSnapshot(RankingType.TREND, malatang, 1, "0.9000", LATEST);
        insertSnapshot(RankingType.TREND, malaxiangguo, 2, "0.8000", LATEST);
        insertSnapshot(RankingType.TREND, hotpot, 3, "0.7000", LATEST);
        insertSnapshot(RankingType.TREND, chicken, 4, "0.6000", LATEST);
        insertRelation(malatang, hotpot, "0.9000");
        insertRelation(malatang, delivery, "0.8000"); // 배달은 검색 대상(최신 스냅샷)이 아니다

        mockMvc.perform(get(URL).param("q", "  마라탕 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(2))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("마라탕"))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("훠궈"));

        mockMvc.perform(get(URL).param("q", "마라"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(2))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("마라탕"))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("마라샹궈"));
    }

    @Test
    @DisplayName("q 는 대소문자를 구분하지 않고, 공백만 있으면 조건을 걸지 않는다")
    void searchIgnoresCaseAndBlankQuery() throws Exception {
        insertSnapshot(RankingType.TREND, insertKeyword("BTS"), 1, "0.9000", LATEST);
        insertSnapshot(RankingType.TREND, insertKeyword("뉴진스"), 2, "0.8000", LATEST);

        mockMvc.perform(get(URL).param("q", "bts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(1))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("BTS"));

        mockMvc.perform(get(URL).param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(2));
    }

    @Test
    @DisplayName("categoryId 를 주면 그 카테고리에 속한 키워드만 내려준다")
    void filtersByCategory() throws Exception {
        long food = insertCategory("음식");
        long fashion = insertCategory("패션");
        long malatang = insertKeyword("마라탕");
        long jacket = insertKeyword("바람막이");
        insertSnapshot(RankingType.TREND, malatang, 1, "0.9000", LATEST);
        insertSnapshot(RankingType.TREND, jacket, 2, "0.8000", LATEST);
        insertKeywordCategory(malatang, food);
        insertKeywordCategory(jacket, fashion);

        mockMvc.perform(get(URL).param("categoryId", String.valueOf(food)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(1))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("마라탕"));
    }

    @Test
    @DisplayName("없는 categoryId 를 주면 404 를 내려준다")
    void returns404ForUnknownCategory() throws Exception {
        mockMvc.perform(get(URL).param("categoryId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("해당 카테고리를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    @DisplayName("badgeType 은 최신 스냅샷 row 의 뱃지로 거르고, 여러 개를 주면 하나라도 가진 키워드를 내려준다")
    void filtersByAnyOfBadges() throws Exception {
        long trendNew = insertKeyword("트렌드신규");
        long risingBadge = insertKeyword("급상승뱃지");
        long steady = insertKeyword("꾸준함");
        long oldBadge = insertKeyword("어제뱃지");
        insertBadge(insertSnapshot(RankingType.TREND, trendNew, 1, "0.9000", LATEST), "TREND_NEW");
        insertBadge(insertSnapshot(RankingType.POTENTIAL, risingBadge, 1, "0.8000", LATEST), "RISING");
        insertBadge(insertSnapshot(RankingType.RISING, steady, 1, "0.7000", LATEST), "STEADY");
        insertSnapshot(RankingType.TREND, oldBadge, 2, "0.6000", LATEST);
        insertBadge(insertSnapshot(RankingType.TREND, oldBadge, 1, "0.6000", PREVIOUS), "TREND_NEW");

        mockMvc.perform(get(URL).param("badgeType", "TREND_NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(1))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("트렌드신규"));

        mockMvc.perform(get(URL).param("badgeType", "RISING,STEADY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(2))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("급상승뱃지"))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("꾸준함"));
    }

    @Test
    @DisplayName("알 수 없는 badgeType 은 400 을 내려준다")
    void rejectsUnknownBadgeType() throws Exception {
        mockMvc.perform(get(URL).param("badgeType", "TREND_NEW,NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("잘못된 요청 파라미터입니다."));
    }

    @Test
    @DisplayName("지수 범위는 양 끝값을 포함하고, 범위를 좁히면 그 지수가 없는 키워드는 뺀다")
    void filtersByScoreRanges() throws Exception {
        long both = insertKeyword("둘다");
        long trendOnly = insertKeyword("트렌드만");
        long potentialOnly = insertKeyword("포텐셜만");
        insertSnapshot(RankingType.TREND, both, 1, "0.9000", LATEST);
        insertSnapshot(RankingType.POTENTIAL, both, 2, "0.3000", LATEST);
        insertSnapshot(RankingType.TREND, trendOnly, 2, "0.5000", LATEST);
        insertSnapshot(RankingType.POTENTIAL, potentialOnly, 1, "0.7000", LATEST);

        mockMvc.perform(get(URL).param("trendMin", "50"))
                .andExpect(jsonPath("$.details.totalElements").value(2))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("둘다"))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("트렌드만"));

        mockMvc.perform(get(URL).param("trendMax", "50"))
                .andExpect(jsonPath("$.details.totalElements").value(1))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("트렌드만"));

        mockMvc.perform(get(URL).param("potentialMin", "30").param("potentialMax", "70"))
                .andExpect(jsonPath("$.details.totalElements").value(2))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("둘다"))
                .andExpect(jsonPath("$.details.items[1].keywordName").value("포텐셜만"));
    }

    @Test
    @DisplayName("trendMin 이 trendMax 보다 크면 400 과 보낸 값을 details 로 내려준다")
    void rejectsInvertedTrendRange() throws Exception {
        mockMvc.perform(get(URL).param("trendMin", "90").param("trendMax", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("trendMin은 trendMax보다 클 수 없습니다."))
                .andExpect(jsonPath("$.details.trendMin").value(90))
                .andExpect(jsonPath("$.details.trendMax").value(50));
    }

    @Test
    @DisplayName("potentialMin 이 potentialMax 보다 크면 400 과 보낸 값을 details 로 내려준다")
    void rejectsInvertedPotentialRange() throws Exception {
        mockMvc.perform(get(URL).param("potentialMin", "80").param("potentialMax", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("potentialMin은 potentialMax보다 클 수 없습니다."))
                .andExpect(jsonPath("$.details.potentialMin").value(80))
                .andExpect(jsonPath("$.details.potentialMax").value(20));
    }

    @ParameterizedTest
    @CsvSource({"trendMin,-1", "trendMax,101", "potentialMin,-1", "potentialMax,101", "trendMin,abc"})
    @DisplayName("지수 범위 값이 0~100 밖이거나 숫자가 아니면 공통 400 을 내려준다")
    void rejectsOutOfRangeScore(String name, String value) throws Exception {
        mockMvc.perform(get(URL).param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청 파라미터입니다."));
    }

    @Test
    @DisplayName("risingOnly=true 이면 최신 RISING 스냅샷에 든 키워드만 내려준다 (RISING 뱃지와는 별개)")
    void filtersRisingSnapshotKeywords() throws Exception {
        long rising = insertKeyword("라이징랭킹");
        long badgeOnly = insertKeyword("급상승뱃지만");
        insertSnapshot(RankingType.RISING, rising, 1, "0.4000", LATEST);
        insertBadge(insertSnapshot(RankingType.TREND, badgeOnly, 1, "0.9000", LATEST), "RISING");

        mockMvc.perform(get(URL).param("risingOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.totalElements").value(1))
                .andExpect(jsonPath("$.details.items[0].keywordName").value("라이징랭킹"));
    }

    private long insertKeyword(String name) {
        return insertKeyword(name, LocalDateTime.of(2026, 7, 1, 0, 0));
    }

    private long insertKeyword(String name, LocalDateTime firstCollectedAt) {
        jdbcTemplate.update(
                "INSERT INTO KEYWORD (keyword_name, first_collected_at) VALUES (?, ?)", name, firstCollectedAt);
        return jdbcTemplate.queryForObject("SELECT id FROM KEYWORD WHERE keyword_name = ?", Long.class, name);
    }

    private long insertSnapshot(RankingType rankingType, long keywordId, int rank, String score,
                                LocalDateTime snapshotAt) {
        jdbcTemplate.update(
                "INSERT INTO RANKING_SNAPSHOT (ranking_type, keyword_id, rank_position, trend_score, snapshot_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                rankingType.name(), keywordId, rank, new BigDecimal(score), snapshotAt);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM RANKING_SNAPSHOT WHERE ranking_type = ? AND keyword_id = ? AND snapshot_at = ?",
                Long.class, rankingType.name(), keywordId, snapshotAt);
    }

    private void insertRelation(long keywordId, long relatedKeywordId, String relevance) {
        jdbcTemplate.update(
                "INSERT INTO KEYWORD_RELATION (keyword_id, related_keyword_id, relevance_score) VALUES (?, ?, ?)",
                keywordId, relatedKeywordId, new BigDecimal(relevance));
    }

    private long insertCategory(String name) {
        jdbcTemplate.update("INSERT INTO CATEGORY (name) VALUES (?)", name);
        return jdbcTemplate.queryForObject("SELECT id FROM CATEGORY WHERE name = ?", Long.class, name);
    }

    private void insertKeywordCategory(long keywordId, long categoryId) {
        jdbcTemplate.update("INSERT INTO KEYWORD_CATEGORY (keyword_id, category_id) VALUES (?, ?)", keywordId, categoryId);
    }

    private void insertBadge(long rankingSnapshotId, String badgeType) {
        jdbcTemplate.update(
                "INSERT INTO RANKING_SNAPSHOT_BADGE (ranking_snapshot_id, badge_type) VALUES (?, ?)",
                rankingSnapshotId, badgeType);
    }
}
