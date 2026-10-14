package com.djk.domain.keyword;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestContainer.class)
class RecommendedKeywordApiTest {

    private static final String URL = "/api/v1/keywords/recommended";
    private static final LocalDateTime LATEST = LocalDateTime.of(2026, 7, 24, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM RANKING_SNAPSHOT_BADGE");
        jdbcTemplate.update("DELETE FROM RANKING_SNAPSHOT");
        jdbcTemplate.update("DELETE FROM KEYWORD");
    }

    @Test
    @DisplayName("limit 을 생략하면 TREND 최신 스냅샷의 1위부터 10개를 순위 순서로 내려준다")
    void returnsTop10OfLatestTrendSnapshotByDefault() throws Exception {
        for (int rank = 12; rank >= 1; rank--) {
            long keywordId = insertKeyword("키워드" + rank);
            insertSnapshot("TREND", keywordId, rank, new BigDecimal("0.5000"), LATEST);
        }

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("추천 검색어를 조회하였습니다."))
                .andExpect(jsonPath("$.details.snapshotAt").value("2026-07-24T09:00:00"))
                .andExpect(jsonPath("$.details.keywords.length()").value(10))
                .andExpect(jsonPath("$.details.keywords[0].rankPosition").value(1))
                .andExpect(jsonPath("$.details.keywords[0].keywordName").value("키워드1"))
                .andExpect(jsonPath("$.details.keywords[9].rankPosition").value(10))
                .andExpect(jsonPath("$.details.keywords[9].keywordName").value("키워드10"));
    }

    private long insertKeyword(String name) {
        jdbcTemplate.update(
                "INSERT INTO KEYWORD (keyword_name, first_collected_at) VALUES (?, ?)",
                name, LocalDateTime.of(2026, 7, 1, 0, 0));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM KEYWORD WHERE keyword_name = ?", Long.class, name);
    }

    private void insertSnapshot(String rankingType, long keywordId, int rank, BigDecimal score,
                                LocalDateTime snapshotAt) {
        jdbcTemplate.update(
                "INSERT INTO RANKING_SNAPSHOT (ranking_type, keyword_id, rank_position, trend_score, snapshot_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                rankingType, keywordId, rank, score, snapshotAt);
    }
}
