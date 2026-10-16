package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchResponse;
import com.djk.domain.keyword.service.KeywordSearchService;
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
public class KeywordSearchController {

    private final KeywordSearchService keywordSearchService;

    @GetMapping("/search")
    public ApiResponse<KeywordSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        KeywordSearchCondition condition = new KeywordSearchCondition(q, categoryId, page, size);
        return ApiResponse.success("검색 결과를 조회하였습니다.",
                KeywordSearchResponse.from(keywordSearchService.search(condition)));
    }
}
