package com.djk.domain.keyword.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 키워드 상세 검색 결과 한 페이지. 점수는 DB 스케일(0~1) 그대로다.
 */
public record KeywordSearchResult(
        LocalDateTime snapshotAt, int page, int size, long totalElements, List<KeywordSearchHit> hits) {

    public static KeywordSearchResult empty(LocalDateTime snapshotAt, KeywordSearchCondition condition) {
        return new KeywordSearchResult(snapshotAt, condition.page(), condition.size(), 0, List.of());
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
