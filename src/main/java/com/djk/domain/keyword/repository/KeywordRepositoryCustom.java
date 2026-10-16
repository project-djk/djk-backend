package com.djk.domain.keyword.repository;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchRow;
import com.djk.domain.ranking.dto.SnapshotTimes;
import java.util.List;

/** QueryDSL 로 작성하는 조회 (ADR-0005). */
public interface KeywordRepositoryCustom {

    /** 검색 대상 키워드(최신 TREND·POTENTIAL·RISING 스냅샷에 든 키워드) 중 조건에 맞는 한 페이지. */
    List<KeywordSearchRow> search(KeywordSearchCondition condition, SnapshotTimes latest);

    /** search 와 같은 조건의 전체 개수. */
    long countSearch(KeywordSearchCondition condition, SnapshotTimes latest);
}
