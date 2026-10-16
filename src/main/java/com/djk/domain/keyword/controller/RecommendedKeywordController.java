package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.RecommendedKeywordsResponse;
import com.djk.domain.ranking.dto.LatestRanking;
import com.djk.domain.ranking.entity.RankingType;
import com.djk.domain.ranking.service.RankingQueryService;
import com.djk.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class RecommendedKeywordController {

    private final RankingQueryService rankingQueryService;

    /** 메인 추천 검색어 = TREND 최신 스냅샷의 1위부터 limit 개. */
    @GetMapping("/recommended")
    public ApiResponse<RecommendedKeywordsResponse> getRecommended(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        LatestRanking ranking = rankingQueryService.getLatestTop(RankingType.TREND, limit);
        String message = ranking.isEmpty() ? "추천 검색어가 없습니다." : "추천 검색어를 조회하였습니다.";
        return ApiResponse.success(message, RecommendedKeywordsResponse.from(ranking));
    }
}
