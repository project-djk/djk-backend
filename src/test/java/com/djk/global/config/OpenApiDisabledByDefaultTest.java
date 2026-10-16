package com.djk.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.djk.support.MySqlTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** 운영처럼 local·dev 프로파일이 없으면 API 문서를 노출하지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestContainer.class)
class OpenApiDisabledByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    @DisplayName("프로파일을 지정하지 않으면 API 문서와 Swagger UI 는 404 다")
    void hidesApiDocsWithoutProfile(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }
}
