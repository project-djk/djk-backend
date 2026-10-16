package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchResponse;
import com.djk.domain.keyword.dto.KeywordSearchSort;
import com.djk.domain.keyword.dto.ScoreRange;
import com.djk.domain.keyword.dto.TrendPeriod;
import com.djk.domain.keyword.service.KeywordSearchService;
import com.djk.domain.ranking.entity.BadgeType;
import com.djk.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class KeywordSearchController {

    private final KeywordSearchService keywordSearchService;

    @GetMapping("/search")
    public ApiResponse<KeywordSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<BadgeType> badgeType,
            @RequestParam(defaultValue = "7D") @Pattern(regexp = TrendPeriod.CODE_PATTERN) String period,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100) int trendMin,
            @RequestParam(defaultValue = "100") @Min(0) @Max(100) int trendMax,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100) int potentialMin,
            @RequestParam(defaultValue = "100") @Min(0) @Max(100) int potentialMax,
            @RequestParam(defaultValue = "false") boolean risingOnly,
            @RequestParam(defaultValue = "TREND") KeywordSearchSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        KeywordSearchCondition condition = new KeywordSearchCondition(
                q, categoryId, badgeType, TrendPeriod.fromCode(period),
                ScoreRange.of("trend", trendMin, trendMax),
                ScoreRange.of("potential", potentialMin, potentialMax),
                risingOnly, sort, page, size);
        return ApiResponse.success("검색 결과를 조회하였습니다.",
                KeywordSearchResponse.from(keywordSearchService.search(condition)));
    }
}
