package com.djk.domain.keyword;

import com.djk.domain.ranking.RankingQueryService;
import com.djk.domain.ranking.RankingType;
import com.djk.global.response.ApiResponse;
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
            @RequestParam(defaultValue = "10") int limit) {
        RecommendedKeywordsResponse response =
                RecommendedKeywordsResponse.from(rankingQueryService.getLatestTop(RankingType.TREND, limit));
        return ApiResponse.success("추천 검색어를 조회하였습니다.", response);
    }
}
