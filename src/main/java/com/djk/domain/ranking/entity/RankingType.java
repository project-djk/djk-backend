package com.djk.domain.ranking.entity;

/**
 * 랭킹 유형. DB 의 RANKING_SNAPSHOT.ranking_type ENUM 과 값이 같아야 한다.
 */
public enum RankingType {
    TREND,
    RISING,
    POTENTIAL
}
