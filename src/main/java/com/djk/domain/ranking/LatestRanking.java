package com.djk.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 한 랭킹 유형의 최신 스냅샷에서 위에서부터 자른 row 들. 스냅샷이 없으면 snapshotAt 은 null 이고 entries 는 비어 있다.
 */
public record LatestRanking(LocalDateTime snapshotAt, List<RankingSnapshot> entries) {

    public static LatestRanking empty() {
        return new LatestRanking(null, List.of());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
