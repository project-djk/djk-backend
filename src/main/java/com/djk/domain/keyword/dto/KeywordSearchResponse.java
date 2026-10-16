package com.djk.domain.keyword.dto;

import com.djk.domain.ranking.entity.BadgeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record KeywordSearchResponse(
        @Schema(description = "어느 집계의 결과인지. 최신 TREND 스냅샷 기준 (KST, 오프셋 없음). 스냅샷이 없으면 null",
                example = "2026-07-24T09:00:00", nullable = true)
        LocalDateTime snapshotAt,
        @Schema(description = "페이지 번호 (0부터)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "조건에 맞는 전체 키워드 수", example = "8") long totalElements,
        @Schema(description = "전체 페이지 수", example = "1") int totalPages,
        List<Item> items) {

    @Schema(name = "KeywordSearchItem")
    public record Item(
            @Schema(example = "12") Long keywordId,
            @Schema(example = "마라탕") String keywordName,
            @Schema(description = "최신 TREND·POTENTIAL·RISING 스냅샷 row 에 붙은 뱃지 (중복 없음)", example = "[\"TREND_NEW\"]")
            List<BadgeType> badges,
            @Schema(description = "최신 TREND 스냅샷에서의 순위. 순위권 밖이면 null", nullable = true) Trend trend,
            @Schema(description = "최신 POTENTIAL 스냅샷에서의 순위. 순위권 밖이면 null", nullable = true) Potential potential) {

        static Item from(KeywordSearchHit hit) {
            return new Item(
                    hit.keyword().getId(),
                    hit.keyword().getKeywordName(),
                    hit.badges(),
                    hit.trend() == null ? null : Trend.from(hit.trend()),
                    hit.potential() == null ? null : Potential.from(hit.potential()));
        }
    }

    @Schema(name = "KeywordSearchTrend")
    public record Trend(
            @Schema(example = "5") int rankPosition,
            @Schema(description = "직전 스냅샷 순위 - 현재 순위. 양수가 상승. 직전 스냅샷에 없었으면 null",
                    example = "2", nullable = true)
            Integer rankChange,
            @Schema(description = "트렌드 지수 (0~100, 반올림하지 않음)", example = "82.34") BigDecimal trendScore) {

        static Trend from(RankEntry entry) {
            return new Trend(entry.snapshot().getRankPosition(), entry.rankChange(),
                    toScore100(entry.snapshot().getTrendScore()));
        }
    }

    @Schema(name = "KeywordSearchPotential")
    public record Potential(
            @Schema(example = "9") int rankPosition,
            @Schema(description = "직전 스냅샷 순위 - 현재 순위. 양수가 상승. 직전 스냅샷에 없었으면 null",
                    example = "-1", nullable = true)
            Integer rankChange,
            @Schema(description = "Potential 점수 (0~100, 반올림하지 않음)", example = "61.00") BigDecimal score) {

        static Potential from(RankEntry entry) {
            return new Potential(entry.snapshot().getRankPosition(), entry.rankChange(),
                    toScore100(entry.snapshot().getTrendScore()));
        }
    }

    public static KeywordSearchResponse from(KeywordSearchResult result) {
        return new KeywordSearchResponse(
                result.snapshotAt(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hits().stream().map(Item::from).toList());
    }

    /** 0~1 → 0~100. 이 API 의 점수 환산은 여기 한 곳에서만 한다. */
    private static BigDecimal toScore100(BigDecimal score) {
        return score.movePointRight(2);
    }
}
