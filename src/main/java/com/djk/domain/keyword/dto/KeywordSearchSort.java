package com.djk.domain.keyword.dto;

/**
 * 상세 검색 정렬 기준. 값이 없는 키워드는 맨 뒤, 동점이면 keywordId 오름차순이다.
 */
public enum KeywordSearchSort {
    /** 최신 TREND 스냅샷의 트렌드 지수 높은 순. */
    TREND,
    /** 최신 POTENTIAL 스냅샷의 Potential 점수 높은 순. */
    POTENTIAL,
    /** 최초 수집 일시가 최근인 순. */
    LATEST
}
