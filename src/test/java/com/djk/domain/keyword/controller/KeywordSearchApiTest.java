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
}
