package com.djk.domain.keyword.dto;

import com.djk.domain.ranking.entity.BadgeType;
import java.util.List;

/**
 * 키워드 상세 검색 조건. 컨트롤러에서 검증을 마친 값이다.
 *
 * @param query      앞뒤 공백을 지운 검색어. 비어 있으면 null (조건 없음)
 * @param categoryId null 이면 조건 없음
 * @param badgeTypes 이 중 하나라도 가진 키워드. 비어 있으면 조건 없음
 * @param period     trendLine 의 기간. 키워드를 거르지 않는다
 * @param trendRange 최신 TREND 스냅샷의 트렌드 지수 범위
 * @param potentialRange 최신 POTENTIAL 스냅샷의 Potential 점수 범위
 * @param risingOnly 최신 RISING 스냅샷에 든 키워드만
 */
public record KeywordSearchCondition(
        String query,
        Long categoryId,
        List<BadgeType> badgeTypes,
        TrendPeriod period,
        ScoreRange trendRange,
        ScoreRange potentialRange,
        boolean risingOnly,
        int page,
        int size) {

    public KeywordSearchCondition {
        query = query == null || query.isBlank() ? null : query.strip();
        badgeTypes = badgeTypes == null ? List.of() : List.copyOf(badgeTypes);
    }

    public long offset() {
        return (long) page * size;
    }
}
