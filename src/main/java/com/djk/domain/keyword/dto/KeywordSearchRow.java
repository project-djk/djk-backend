package com.djk.domain.keyword.dto;

import com.djk.domain.keyword.entity.Keyword;
import com.djk.domain.ranking.entity.RankingSnapshot;
import java.util.stream.Stream;

/**
 * 검색 조건에 맞은 키워드 하나와, 그 키워드의 최신 TREND·POTENTIAL·RISING 스냅샷 row. 해당 랭킹에 없으면 null.
 */
public record KeywordSearchRow(Keyword keyword, RankingSnapshot trend, RankingSnapshot potential,
                               RankingSnapshot rising) {

    /** null 이 아닌 스냅샷 row 들. */
    public Stream<RankingSnapshot> snapshots() {
        return Stream.of(trend, potential, rising).filter(snapshot -> snapshot != null);
    }
}
