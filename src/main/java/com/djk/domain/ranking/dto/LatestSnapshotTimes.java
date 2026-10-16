package com.djk.domain.ranking.dto;

import com.djk.domain.ranking.entity.RankingType;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 랭킹 유형별 최신 스냅샷의 snapshot_at. 스냅샷이 없는 유형은 값이 없다.
 */
public record LatestSnapshotTimes(Map<RankingType, LocalDateTime> byType) {

    public LatestSnapshotTimes {
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
