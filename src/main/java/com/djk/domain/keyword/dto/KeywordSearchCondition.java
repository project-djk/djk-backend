package com.djk.domain.keyword.dto;

/**
 * 키워드 상세 검색 조건. 컨트롤러에서 검증을 마친 값이다.
 */
public record KeywordSearchCondition(int page, int size) {

    public long offset() {
        return (long) page * size;
    }
}
