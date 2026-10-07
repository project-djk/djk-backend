package com.djk.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("성공 응답은 isSuccess / message / details 필드로 직렬화된다")
    void successResponseUsesSpecFieldNames() throws Exception {
        String body = objectMapper.writeValueAsString(ApiResponse.success("조회 성공", List.of(1, 2)));
        JsonNode json = objectMapper.readTree(body);

        assertThat(json.get("isSuccess").asBoolean()).isTrue();
        assertThat(json.get("message").asText()).isEqualTo("조회 성공");
        assertThat(json.get("details").size()).isEqualTo(2);
        assertThat(json.has("success")).isFalse();
    }

    @Test
    @DisplayName("실패 응답은 isSuccess=false 이고 details 는 null 이다")
    void failResponseHasNoDetails() throws Exception {
        String body = objectMapper.writeValueAsString(ApiResponse.fail("요청에 실패했습니다"));
        JsonNode json = objectMapper.readTree(body);

        assertThat(json.get("isSuccess").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).isEqualTo("요청에 실패했습니다");
        assertThat(json.get("details").isNull()).isTrue();
    }
}
