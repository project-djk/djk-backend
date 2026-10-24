package com.djk.domain.keyword.repository;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchRow;
import com.djk.domain.keyword.entity.KeywordRelation;
import com.djk.domain.keyword.entity.TrendIndexHistory;
import com.djk.domain.ranking.dto.SnapshotTimes;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** QueryDSL 로 작성하는 조회 (ADR-0005). */
public interface KeywordRepositoryCustom {

    /** 검색 대상 키워드(최신 TREND·POTENTIAL·RISING 스냅샷에 든 키워드) 중 조건에 맞는 한 페이지. */
    List<KeywordSearchRow> search(KeywordSearchCondition condition, SnapshotTimes latest);

    /** search 와 같은 조건의 전체 개수. */
    long countSearch(KeywordSearchCondition condition, SnapshotTimes latest);

    /** 키워드별 카테고리 이름 (카테고리 id 순). 카테고리가 없는 키워드는 키가 없다. */
    Map<Long, List<String>> findCategoryNames(Collection<Long> keywordIds);

    /** 키워드별 연관어 이름을 연관도 높은 순으로 최대 limit 개. 연관어가 없는 키워드는 키가 없다. */
    Map<Long, List<String>> findTopRelatedKeywordNames(Collection<Long> keywordIds, int limit);

    /** 이 키워드 기준 연관도가 가장 높은 연관 관계 (연관 키워드 포함). 동점이면 연관 키워드 id 가 작은 쪽. 없으면 null. */
    KeywordRelation findTopRelation(long keywordId);

    /** 키워드별 from 이상 to 이하에 측정된 트렌드 지수 이력 (측정 시각 순). 이력이 없는 키워드는 키가 없다. */
    Map<Long, List<TrendIndexHistory>> findTrendHistories(Collection<Long> keywordIds, LocalDateTime from,
                                                          LocalDateTime to);
}
