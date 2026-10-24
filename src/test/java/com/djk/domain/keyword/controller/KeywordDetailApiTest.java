package com.djk.domain.keyword.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.djk.support.MySqlTestContainer;
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

    private long insertCategory(String name) {
        jdbcTemplate.update("INSERT INTO CATEGORY (name) VALUES (?)", name);
        return jdbcTemplate.queryForObject("SELECT id FROM CATEGORY WHERE name = ?", Long.class, name);
    }

    private void insertKeywordCategory(long keywordId, long categoryId) {
        jdbcTemplate.update("INSERT INTO KEYWORD_CATEGORY (keyword_id, category_id) VALUES (?, ?)", keywordId, categoryId);
    }
}
