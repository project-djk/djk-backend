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
    }

    public record TopRelation(String keywordName, BigDecimal relevanceScore) {
    }

    public static KeywordDetailResponse from(KeywordDetail detail) {
        return new KeywordDetailResponse(
                detail.keyword().getId(),
                detail.keyword().getKeywordName(),
                detail.categories(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
