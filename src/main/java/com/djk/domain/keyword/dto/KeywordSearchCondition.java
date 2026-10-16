package com.djk.domain.keyword.dto;

/**
 * 키워드 상세 검색 조건. 컨트롤러에서 검증을 마친 값이다.
 *
 * @param query      앞뒤 공백을 지운 검색어. 비어 있으면 null (조건 없음)
 * @param categoryId null 이면 조건 없음
 */
public record KeywordSearchCondition(String query, Long categoryId, int page, int size) {

    public KeywordSearchCondition {
        query = query == null || query.isBlank() ? null : query.strip();
    }

    public long offset() {
        return (long) page * size;
    }
}
