package com.djk.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 문서 정보. API 명세의 기준은 노션 "API 명세서"이고, Swagger 는 실제 구현을 보여주는 문서다. (ADR-0004)
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("DJK 트렌드 분석기 API")
                .version("v1")
                .description("""
                        모든 응답은 `{ "isSuccess", "message", "details" }` 형태(ApiResponse)로 감싸져 있다.
                        트렌드 지수는 0~100 점수다. API 명세의 기준은 노션 "API 명세서"다.
                        """));
    }
}
