package com.djk.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.djk.support.MySqlTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(MySqlTestContainer.class)
class OpenApiLocalProfileTest {

    private static final String RECOMMENDED = "$.paths['/api/v1/keywords/recommended'].get";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("local 프로파일에서는 API 문서에 추천 검색어 API 와 limit 제약이 나온다")
    void documentsRecommendedKeywordApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DJK 트렌드 분석기 API"))
                .andExpect(jsonPath(RECOMMENDED + ".summary").value("메인 추천 검색어 조회"))
                .andExpect(jsonPath(RECOMMENDED + ".parameters[0].name").value("limit"))
                .andExpect(jsonPath(RECOMMENDED + ".parameters[0].schema.minimum").value(1))
                .andExpect(jsonPath(RECOMMENDED + ".parameters[0].schema.maximum").value(100))
                .andExpect(jsonPath(RECOMMENDED + ".parameters[0].schema.default").value(10))
                .andExpect(jsonPath(RECOMMENDED + ".responses['400']").exists());
    }

    @Test
    @DisplayName("200 응답 스키마는 ApiResponse 래퍼와 추천 검색어 항목의 예시 값을 보여준다")
    void documentsResponseSchemaWithWrapper() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(RECOMMENDED + ".responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseRecommendedKeywordsResponse"))
                .andExpect(jsonPath("$.components.schemas.ApiResponseRecommendedKeywordsResponse.properties.isSuccess.type")
                        .value("boolean"))
                .andExpect(jsonPath("$.components.schemas.RecommendedKeywordItem.properties.trendScore.example")
                        .value(82.34))
                .andExpect(jsonPath("$.components.schemas.RecommendedKeywordsResponse.properties.snapshotAt.example")
                        .value("2026-07-24T09:00:00"));
    }

    @Test
    @DisplayName("local 프로파일에서는 Swagger UI 를 열 수 있다")
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
