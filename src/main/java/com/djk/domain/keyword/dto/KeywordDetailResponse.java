package com.djk.domain.keyword.dto;

import com.djk.domain.ranking.entity.BadgeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record KeywordDetailResponse(
        Long keywordId,
        String keywordName,
        List<String> categories,
        List<BadgeType> badges,
        BigDecimal searchScore,
        BigDecimal searchScoreChange,
        Rank trend,
        Rank potential,
        TopRelation topRelation,
        LocalDateTime snapshotAt) {

    public record Rank(int rankPosition, Integer rankChange) {

        static Rank from(RankEntry entry) {
            return entry == null ? null : new Rank(entry.snapshot().getRankPosition(), entry.rankChange());
        }
    }

    public record TopRelation(String keywordName, BigDecimal relevanceScore) {
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
                List.of(),
                toScore100(trendScore),
                toScore100(trendScoreChange),
                Rank.from(detail.trend()),
                Rank.from(detail.potential()),
                null,
                detail.snapshotAt());
    }

    /** 0~1 → 0~100. 이 API 의 점수 환산은 여기 한 곳에서만 한다. null 이면 null. */
    private static BigDecimal toScore100(BigDecimal score) {
        return score == null ? null : score.movePointRight(2);
    }
}
