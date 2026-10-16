package com.djk.domain.keyword.dto;

import com.djk.domain.ranking.entity.RankingSnapshot;
import java.util.Map;

/**
 * 한 랭킹 유형의 최신 스냅샷 row 와 순위 변화(CONTEXT.md "순위 변화").
 *
 * @param rankChange 직전 스냅샷 순위 - 현재 순위. 양수가 상승. 직전 스냅샷에 없었으면 null
 */
public record RankEntry(RankingSnapshot snapshot, Integer rankChange) {

    /**
     * @param previousRanks 직전 스냅샷의 키워드별 순위
     * @return 그 랭킹에 없으면(snapshot 이 null) null
     */
    public static RankEntry of(RankingSnapshot snapshot, Map<Long, Integer> previousRanks) {
        if (snapshot == null) {
            return null;
        }
        Integer previousRank = previousRanks.get(snapshot.getKeyword().getId());
        Integer rankChange = previousRank == null ? null : previousRank - snapshot.getRankPosition();
        return new RankEntry(snapshot, rankChange);
    }
}
