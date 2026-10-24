package com.djk.domain.keyword.dto;

import com.djk.domain.keyword.entity.KeywordRelation;
import com.djk.domain.ranking.entity.BadgeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record KeywordDetailResponse(
        @Schema(example = "40") Long keywordId,
        @Schema(example = "두바이 초콜릿") String keywordName,
        @Schema(description = "키워드가 속한 카테고리 이름", example = "[\"디저트\"]") List<String> categories,
        @Schema(description = "최신 TREND·POTENTIAL·RISING 스냅샷 row 에 붙은 뱃지 (중복 없음). 없으면 빈 배열",
                example = "[\"TREND_NEW\", \"RISING\"]")
        List<BadgeType> badges,
        @Schema(description = "검색 지수 = 최신 TREND 스냅샷의 트렌드 지수 (0~100, 반올림하지 않음). TREND 랭킹 밖이면 null",
                example = "96.0", nullable = true)
        BigDecimal searchScore,
        @Schema(description = "직전 TREND 스냅샷 대비 검색 지수 차이 (0~100 스케일). 직전 스냅샷에 없었거나 searchScore 가 null 이면 null",
                example = "76.0", nullable = true)
        BigDecimal searchScoreChange,
        @Schema(description = "최신 TREND 스냅샷에서의 순위. 순위권 밖이면 null", nullable = true) Rank trend,
        @Schema(description = "최신 POTENTIAL 스냅샷에서의 순위. 순위권 밖이면 null", nullable = true) Rank potential,
        @Schema(description = "연관도가 가장 높은 연관어. 동점이면 먼저 등록된 키워드. 연관어가 없으면 null", nullable = true)
        TopRelation topRelation,
        @Schema(description = "어느 집계의 결과인지. 최신 TREND 스냅샷 기준 (KST, 오프셋 없음). 스냅샷이 없으면 null",
                example = "2026-07-24T09:00:00", nullable = true)
        LocalDateTime snapshotAt) {

    @Schema(name = "KeywordDetailRank")
    public record Rank(
            @Schema(example = "23") int rankPosition,
            @Schema(description = "직전 스냅샷 순위 - 현재 순위. 양수가 상승. 직전 스냅샷에 없었으면 null",
                    example = "17", nullable = true)
            Integer rankChange) {

        static Rank from(RankEntry entry) {
            return entry == null ? null : new Rank(entry.snapshot().getRankPosition(), entry.rankChange());
        }
    }

    @Schema(name = "KeywordDetailTopRelation")
    public record TopRelation(
            @Schema(example = "피스타치오") String keywordName,
            @Schema(description = "연관도 (0~100, 반올림하지 않음)", example = "98.0") BigDecimal relevanceScore) {

        static TopRelation from(KeywordRelation relation) {
            return relation == null
                    ? null
                    : new TopRelation(relation.getRelatedKeyword().getKeywordName(),
                            toScore100(relation.getRelevanceScore()));
        }
    }

    public static KeywordDetailResponse from(KeywordDetail detail) {
        BigDecimal trendScore = detail.trend() == null ? null : detail.trend().snapshot().getTrendScore();
        BigDecimal trendScoreChange = trendScore == null || detail.previousTrendScore() == null
                ? null
                : trendScore.subtract(detail.previousTrendScore());
        return new KeywordDetailResponse(
                detail.keyword().getId(),
                detail.keyword().getKeywordName(),
                detail.categories(),
                detail.badges(),
                toScore100(trendScore),
                toScore100(trendScoreChange),
                Rank.from(detail.trend()),
                Rank.from(detail.potential()),
                TopRelation.from(detail.topRelation()),
                detail.snapshotAt());
    }

    /** 0~1 → 0~100. 이 API 의 점수 환산은 여기 한 곳에서만 한다. null 이면 null. */
    private static BigDecimal toScore100(BigDecimal score) {
        return score == null ? null : score.movePointRight(2);
    }
}
