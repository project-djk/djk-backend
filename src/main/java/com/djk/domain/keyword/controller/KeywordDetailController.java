package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.KeywordDetailResponse;
import com.djk.domain.keyword.service.KeywordDetailService;
import com.djk.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "키워드", description = "키워드 조회 API")
@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class KeywordDetailController {

    private final KeywordDetailService keywordDetailService;

    @Operation(
            summary = "키워드 상세 조회",
            description = """
                    키워드 상세 페이지 상단 영역. 기본 정보, 트렌드/Potential 순위와 순위 변화, 대표 연관어, 뱃지, 검색 지수를 내려준다.
                    순위와 뱃지는 유형별 최신 스냅샷 기준이고, 변화는 같은 유형의 직전 스냅샷과 비교한다.
                    랭킹에 없는 키워드도 200 이며, 해당 데이터가 없는 필드는 null 이다.""")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공. 응답 스키마와 예시는 반환 타입에서 생성된다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "keywordId 가 1 이상의 숫자가 아님",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = """
                            {"isSuccess": false, "message": "잘못된 요청 파라미터입니다.", "details": null}
                            """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "keywordId 에 해당하는 키워드가 없음",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = """
                            {"isSuccess": false, "message": "해당 키워드를 찾을 수 없습니다.", "details": null}
                            """)))
    @GetMapping("/{keywordId}")
    public ApiResponse<KeywordDetailResponse> getDetail(
            @Parameter(description = "키워드 고유 ID (1 이상)", example = "40")
            @PathVariable @Min(1) long keywordId) {
        return ApiResponse.success("키워드 상세 정보를 조회하였습니다.",
                KeywordDetailResponse.from(keywordDetailService.getDetail(keywordId)));
    }
}
