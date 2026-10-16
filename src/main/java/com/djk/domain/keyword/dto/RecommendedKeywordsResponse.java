package com.djk.domain.keyword.dto;

import com.djk.domain.ranking.dto.LatestRanking;
import com.djk.domain.ranking.entity.RankingSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecommendedKeywordsResponse(
        @Schema(description = "어느 집계의 결과인지 (KST, 오프셋 없음). 스냅샷이 없으면 null",
                example = "2026-07-24T09:00:00", nullable = true)
        LocalDateTime snapshotAt,
        @Schema(description = "1위부터 순위 순서")
        List<Item> keywords) {

    @Schema(name = "RecommendedKeywordItem")
    public record Item(
            @Schema(example = "12") Long keywordId,
            @Schema(example = "마라탕") String keywordName,
            @Schema(description = "트렌드 지수 (0~100, 반올림하지 않음)", example = "82.34") BigDecimal trendScore,
            @Schema(example = "1") int rankPosition) {

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
