package com.djk.domain.ranking.dto;

import com.djk.domain.ranking.entity.RankingType;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 랭킹 유형별 스냅샷 하나의 snapshot_at (예: 유형별 최신 스냅샷). 스냅샷이 없는 유형은 값이 없다.
 */
public record SnapshotTimes(Map<RankingType, LocalDateTime> byType) {

    public SnapshotTimes {
        byType = Map.copyOf(byType);
    }

    /** 그 유형의 스냅샷이 없으면 null. */
    public LocalDateTime of(RankingType rankingType) {
        return byType.get(rankingType);
    }

    public boolean isEmpty() {
        return byType.isEmpty();
    }
}
