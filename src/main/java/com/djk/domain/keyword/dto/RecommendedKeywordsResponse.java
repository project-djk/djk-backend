package com.djk.domain.keyword.dto;

import com.djk.domain.ranking.dto.LatestRanking;
import com.djk.domain.ranking.entity.RankingSnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecommendedKeywordsResponse(LocalDateTime snapshotAt, List<Item> keywords) {

    public record Item(Long keywordId, String keywordName, BigDecimal trendScore, int rankPosition) {

        static Item from(RankingSnapshot snapshot) {
            return new Item(
                    snapshot.getKeyword().getId(),
                    snapshot.getKeyword().getKeywordName(),
                    snapshot.getTrendScore().movePointRight(2),
                    snapshot.getRankPosition());
        }
    }

    public static RecommendedKeywordsResponse from(LatestRanking ranking) {
        return new RecommendedKeywordsResponse(
                ranking.snapshotAt(),
                ranking.entries().stream().map(Item::from).toList());
    }
}
