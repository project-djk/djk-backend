package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.RecommendedKeywordsResponse;
import com.djk.domain.ranking.dto.LatestRanking;
import com.djk.domain.ranking.entity.RankingType;
import com.djk.domain.ranking.service.RankingQueryService;
import com.djk.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "키워드", description = "키워드 조회 API")
@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class RecommendedKeywordController {

    private final RankingQueryService rankingQueryService;

    /** 메인 추천 검색어 = TREND 최신 스냅샷의 1위부터 limit 개. */
    @Operation(
            summary = "메인 추천 검색어 조회",
            description = "TREND 랭킹 최신 스냅샷의 1위부터 limit 개를 내려준다. 스냅샷이 없으면 snapshotAt=null, keywords=[] 이다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공 (결과가 없어도 200)",
            content = @Content(examples = @ExampleObject(value = """
                    {"isSuccess": true, "message": "추천 검색어를 조회하였습니다.",
                     "details": {"snapshotAt": "2026-07-24T09:00:00",
                                 "keywords": [{"keywordId": 12, "keywordName": "마라탕", "trendScore": 82.34, "rankPosition": 1}]}}
                    """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "limit 이 1~100 밖이거나 숫자가 아님",
            content = @Content(examples = @ExampleObject(value = """
                    {"isSuccess": false, "message": "잘못된 요청 파라미터입니다.", "details": null}
                    """)))
    @GetMapping("/recommended")
    public ApiResponse<RecommendedKeywordsResponse> getRecommended(
            @Parameter(description = "추천 검색어 개수. 생략하거나 빈 값이면 10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        LatestRanking ranking = rankingQueryService.getLatestTop(RankingType.TREND, limit);
        String message = ranking.isEmpty() ? "추천 검색어가 없습니다." : "추천 검색어를 조회하였습니다.";
        return ApiResponse.success(message, RecommendedKeywordsResponse.from(ranking));
    }
}
