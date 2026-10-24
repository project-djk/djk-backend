package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.KeywordDetailResponse;
import com.djk.domain.keyword.service.KeywordDetailService;
import com.djk.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class KeywordDetailController {

    private final KeywordDetailService keywordDetailService;

    @GetMapping("/{keywordId}")
    public ApiResponse<KeywordDetailResponse> getDetail(@PathVariable @Positive long keywordId) {
        return ApiResponse.success("키워드 상세 정보를 조회하였습니다.",
                KeywordDetailResponse.from(keywordDetailService.getDetail(keywordId)));
    }
}
