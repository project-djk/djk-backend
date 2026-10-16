package com.djk.domain.keyword.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.djk.domain.ranking.entity.RankingType;
import com.djk.support.MySqlTestContainer;
import jakarta.persistence.EntityManagerFactory;
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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(MySqlTestContainer.class)
class RecommendedKeywordApiTest {

    private static final String URL = "/api/v1/keywords/recommended";
    private static final LocalDateTime LATEST = LocalDateTime.of(2026, 7, 24, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM RANKING_SNAPSHOT_BADGE");
        jdbcTemplate.update("DELETE FROM RANKING_SNAPSHOT");
        jdbcTemplate.update("DELETE FROM KEYWORD");
    }

    @Test
    @DisplayName("limit 을 생략하면 TREND 최신 스냅샷의 1위부터 10개를 순위 순서로 내려준다")
    void returnsTop10OfLatestTrendSnapshotByDefault() throws Exception {
        insertTrendRows(12, LATEST);

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

    @Test
    @DisplayName("TREND 스냅샷이 하나도 없으면 200 과 빈 목록, snapshotAt=null 을 내려준다")
    void returnsEmptyWhenNoTrendSnapshot() throws Exception {
        long keywordId = insertKeyword("포텐셜만");
        insertSnapshot(RankingType.POTENTIAL, keywordId, 1, new BigDecimal("0.9000"), LATEST);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("추천 검색어가 없습니다."))
                .andExpect(jsonPath("$.details.snapshotAt").value(nullValue()))
                .andExpect(jsonPath("$.details.keywords").isArray())
                .andExpect(jsonPath("$.details.keywords").isEmpty());
    }

    @Test
    @DisplayName("이전 TREND 스냅샷과 RISING/POTENTIAL 랭킹은 섞지 않는다")
    void usesOnlyLatestTrendSnapshot() throws Exception {
        long old = insertKeyword("어제1위");
        long latest = insertKeyword("오늘1위");
        long rising = insertKeyword("라이징1위");
        long potential = insertKeyword("포텐셜1위");
        insertSnapshot(RankingType.TREND, old, 1, new BigDecimal("0.9900"), LATEST.minusDays(1));
        insertSnapshot(RankingType.TREND, latest, 1, new BigDecimal("0.8000"), LATEST);
        insertSnapshot(RankingType.RISING, rising, 1, new BigDecimal("0.9500"), LATEST.plusDays(1));
        insertSnapshot(RankingType.POTENTIAL, potential, 1, new BigDecimal("0.9500"), LATEST);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.snapshotAt").value("2026-07-24T09:00:00"))
                .andExpect(jsonPath("$.details.keywords.length()").value(1))
                .andExpect(jsonPath("$.details.keywords[0].keywordName").value("오늘1위"));
    }

    @Test
    @DisplayName("최신 스냅샷의 row 가 limit 보다 적으면 있는 만큼만 내려준다")
    void returnsOnlyExistingRowsWhenFewerThanLimit() throws Exception {
        insertSnapshot(RankingType.TREND, insertKeyword("이전스냅샷"), 4, new BigDecimal("0.5000"), LATEST.minusDays(1));
        insertTrendRows(3, LATEST);

        mockMvc.perform(get(URL).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.keywords.length()").value(3))
                .andExpect(jsonPath("$.details.keywords[2].rankPosition").value(3));
    }

    @Test
    @DisplayName("limit 만큼만 잘라서 내려준다")
    void respectsLimit() throws Exception {
        insertTrendRows(5, LATEST);

        mockMvc.perform(get(URL).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.keywords.length()").value(3))
                .andExpect(jsonPath("$.details.keywords[2].keywordName").value("키워드3"));
    }

    @Test
    @DisplayName("트렌드 지수는 0~1 값을 100점으로 환산하되 반올림하지 않는다")
    void convertsTrendScoreTo100ScaleWithoutRounding() throws Exception {
        long keywordId = insertKeyword("마라탕");
        insertSnapshot(RankingType.TREND, keywordId, 1, new BigDecimal("0.8234"), LATEST);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.keywords[0].keywordId").value(keywordId))
                .andExpect(jsonPath("$.details.keywords[0].trendScore").value(82.34));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101", "abc"})
    @DisplayName("limit 이 1~100 밖이거나 숫자가 아니면 400 을 내려준다")
    void rejectsInvalidLimit(String limit) throws Exception {
        mockMvc.perform(get(URL).param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("잘못된 요청 파라미터입니다."))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "100"})
    @DisplayName("limit 의 경계값 1 과 100 은 허용한다")
    void acceptsBoundaryLimit(String limit) throws Exception {
        mockMvc.perform(get(URL).param("limit", limit))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("추천 검색어 조회는 키워드 정보까지 SQL 1번으로 끝난다 (N+1 없음)")
    void loadsRecommendedKeywordsWithSingleQuery() throws Exception {
        insertTrendRows(10, LATEST);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.keywords[9].keywordName").value("키워드10"));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    /** TREND 스냅샷에 "키워드1"~"키워드{count}" 를 1위부터 넣는다. 정렬을 검증하도록 역순으로 INSERT 한다. */
    private void insertTrendRows(int count, LocalDateTime snapshotAt) {
        for (int rank = count; rank >= 1; rank--) {
            insertSnapshot(RankingType.TREND, insertKeyword("키워드" + rank), rank, new BigDecimal("0.5000"), snapshotAt);
        }
    }

    private long insertKeyword(String name) {
        jdbcTemplate.update(
                "INSERT INTO KEYWORD (keyword_name, first_collected_at) VALUES (?, ?)",
                name, LocalDateTime.of(2026, 7, 1, 0, 0));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM KEYWORD WHERE keyword_name = ?", Long.class, name);
    }

    private void insertSnapshot(RankingType rankingType, long keywordId, int rank, BigDecimal score,
                                LocalDateTime snapshotAt) {
        jdbcTemplate.update(
                "INSERT INTO RANKING_SNAPSHOT (ranking_type, keyword_id, rank_position, trend_score, snapshot_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                rankingType.name(), keywordId, rank, score, snapshotAt);
    }
}
