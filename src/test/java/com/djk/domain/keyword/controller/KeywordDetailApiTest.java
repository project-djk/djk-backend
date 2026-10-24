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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestContainer.class)
class KeywordDetailApiTest {

    private static final String URL = "/api/v1/keywords/{keywordId}";
    private static final LocalDateTime LATEST = LocalDateTime.of(2026, 7, 24, 9, 0);
    private static final LocalDateTime PREVIOUS = LATEST.minusDays(1);
    private static final LocalDateTime OLDER = LATEST.minusDays(2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        // KEYWORD 를 지우면 FK ON DELETE CASCADE 로 스냅샷·뱃지·연관어·카테고리 매핑도 지워진다.
        jdbcTemplate.update("DELETE FROM KEYWORD");
        jdbcTemplate.update("DELETE FROM CATEGORY");
    }

    @Test
    @DisplayName("어느 랭킹에도 없는 키워드는 기본 정보만 채우고 나머지는 null 이나 빈 배열로 내려준다")
    void returnsBasicInfoForUnrankedKeyword() throws Exception {
        long keywordId = insertKeyword("두바이 초콜릿");
        long food = insertCategory("음식");
        long dessert = insertCategory("디저트");
        insertKeywordCategory(keywordId, dessert);
        insertKeywordCategory(keywordId, food);

        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("키워드 상세 정보를 조회하였습니다."))
                .andExpect(jsonPath("$.details.keywordId").value(keywordId))
                .andExpect(jsonPath("$.details.keywordName").value("두바이 초콜릿"))
                .andExpect(jsonPath("$.details.categories[0]").value("음식"))
                .andExpect(jsonPath("$.details.categories[1]").value("디저트"))
                .andExpect(jsonPath("$.details.badges").isEmpty())
                .andExpect(jsonPath("$.details.searchScore").value(nullValue()))
                .andExpect(jsonPath("$.details.searchScoreChange").value(nullValue()))
                .andExpect(jsonPath("$.details.trend").value(nullValue()))
                .andExpect(jsonPath("$.details.potential").value(nullValue()))
                .andExpect(jsonPath("$.details.topRelation").value(nullValue()))
                .andExpect(jsonPath("$.details.snapshotAt").value(nullValue()));
    }

    @Test
    @DisplayName("최신 TREND·POTENTIAL 스냅샷의 순위와 직전 스냅샷 대비 순위 변화, 검색 지수와 그 변화량을 내려준다")
    void returnsRanksAndSearchScoreComparedWithPreviousSnapshot() throws Exception {
        long keywordId = insertKeyword("두바이 초콜릿");
        insertSnapshot(RankingType.TREND, keywordId, 50, "0.1000", OLDER);
        insertSnapshot(RankingType.TREND, keywordId, 40, "0.2000", PREVIOUS);
        insertSnapshot(RankingType.TREND, keywordId, 23, "0.9600", LATEST);
        insertSnapshot(RankingType.POTENTIAL, keywordId, 78, "0.3000", PREVIOUS);
        insertSnapshot(RankingType.POTENTIAL, keywordId, 81, "0.2900", LATEST);

        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.searchScore").value(96.0))
                .andExpect(jsonPath("$.details.searchScoreChange").value(76.0))
                .andExpect(jsonPath("$.details.trend.rankPosition").value(23))
                .andExpect(jsonPath("$.details.trend.rankChange").value(17))
                .andExpect(jsonPath("$.details.potential.rankPosition").value(81))
                .andExpect(jsonPath("$.details.potential.rankChange").value(-3))
                .andExpect(jsonPath("$.details.snapshotAt").value("2026-07-24T09:00:00"));
    }

    @Test
    @DisplayName("직전 스냅샷에 없던 키워드는 rankChange 와 searchScoreChange 가 null 이다")
    void returnsNullChangesWhenAbsentFromPreviousSnapshot() throws Exception {
        long keywordId = insertKeyword("두바이 초콜릿");
        long other = insertKeyword("마라탕");
        insertSnapshot(RankingType.TREND, other, 1, "0.9000", PREVIOUS);
        insertSnapshot(RankingType.TREND, keywordId, 5, "0.8234", LATEST);

        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.searchScore").value(82.34))
                .andExpect(jsonPath("$.details.searchScoreChange").value(nullValue()))
                .andExpect(jsonPath("$.details.trend.rankPosition").value(5))
                .andExpect(jsonPath("$.details.trend.rankChange").value(nullValue()))
                .andExpect(jsonPath("$.details.potential").value(nullValue()));
    }

    @Test
    @DisplayName("최신 TREND 스냅샷에 없으면 searchScore·trend 는 null 이고, snapshotAt 은 최신 TREND 스냅샷 시각이다")
    void returnsNullSearchScoreWhenOutOfLatestTrendRanking() throws Exception {
        long keywordId = insertKeyword("두바이 초콜릿");
        insertSnapshot(RankingType.TREND, keywordId, 3, "0.9000", PREVIOUS);
        insertSnapshot(RankingType.TREND, insertKeyword("마라탕"), 1, "0.9000", LATEST);
        insertSnapshot(RankingType.POTENTIAL, keywordId, 2, "0.7000", LATEST);

        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.searchScore").value(nullValue()))
                .andExpect(jsonPath("$.details.searchScoreChange").value(nullValue()))
                .andExpect(jsonPath("$.details.trend").value(nullValue()))
                .andExpect(jsonPath("$.details.potential.rankPosition").value(2))
                .andExpect(jsonPath("$.details.potential.rankChange").value(nullValue()))
                .andExpect(jsonPath("$.details.snapshotAt").value("2026-07-24T09:00:00"));
    }

    @Test
    @DisplayName("최신 TREND·POTENTIAL·RISING 스냅샷 row 의 뱃지를 중복 없이 모으고, 이전 스냅샷의 뱃지는 뺀다")
    void collectsBadgesFromLatestSnapshots() throws Exception {
        long keywordId = insertKeyword("두바이 초콜릿");
        insertBadge(insertSnapshot(RankingType.TREND, keywordId, 30, "0.5000", PREVIOUS), "STEADY");
        insertBadge(insertSnapshot(RankingType.TREND, keywordId, 23, "0.9600", LATEST), "TREND_NEW");
        insertBadge(insertSnapshot(RankingType.POTENTIAL, keywordId, 81, "0.2900", LATEST), "POTENTIAL_NEW");
        long rising = insertSnapshot(RankingType.RISING, keywordId, 1, "0.9600", LATEST);
        insertBadge(rising, "RISING");
        insertBadge(rising, "TREND_NEW");

        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.badges.length()").value(3))
                .andExpect(jsonPath("$.details.badges[0]").value("TREND_NEW"))
                .andExpect(jsonPath("$.details.badges[1]").value("POTENTIAL_NEW"))
                .andExpect(jsonPath("$.details.badges[2]").value("RISING"));
    }

    @Test
    @DisplayName("topRelation 은 이 키워드 기준 연관도가 가장 높은 연관어이고, 동점이면 연관 키워드 id 가 작은 쪽이다")
    void returnsTopRelation() throws Exception {
        long keywordId = insertKeyword("두바이 초콜릿");
        long pistachio = insertKeyword("피스타치오");
        long kadaif = insertKeyword("카다이프");
        long chocolate = insertKeyword("초콜릿");
        long reverse = insertKeyword("역방향");
        insertRelation(keywordId, chocolate, "0.5000");
        insertRelation(keywordId, pistachio, "0.9800");
        insertRelation(keywordId, kadaif, "0.9800");
        insertRelation(reverse, keywordId, "0.9900"); // 이 키워드를 연관어로 가진 row 는 보지 않는다

        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.topRelation.keywordName").value("피스타치오"))
                .andExpect(jsonPath("$.details.topRelation.relevanceScore").value(98.0));
    }

    @Test
    @DisplayName("없는 키워드면 404 를 내려준다")
    void returnsNotFoundForUnknownKeyword() throws Exception {
        mockMvc.perform(get(URL, 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("해당 키워드를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    @DisplayName("keywordId 가 1 이상의 숫자가 아니면 400 을 내려준다")
    void rejectsInvalidKeywordId(String keywordId) throws Exception {
        mockMvc.perform(get(URL, keywordId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("잘못된 요청 파라미터입니다."));
    }

    private long insertKeyword(String name) {
        jdbcTemplate.update("INSERT INTO KEYWORD (keyword_name, first_collected_at) VALUES (?, ?)",
                name, LocalDateTime.of(2026, 7, 1, 0, 0));
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

    private void insertBadge(long rankingSnapshotId, String badgeType) {
        jdbcTemplate.update(
                "INSERT INTO RANKING_SNAPSHOT_BADGE (ranking_snapshot_id, badge_type) VALUES (?, ?)",
                rankingSnapshotId, badgeType);
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
}
