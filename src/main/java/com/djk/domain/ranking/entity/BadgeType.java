package com.djk.domain.ranking.entity;

/**
 * 랭킹 스냅샷 row 에 붙는 뱃지. DB 의 RANKING_SNAPSHOT_BADGE.badge_type ENUM 과 값이 같아야 한다.
 */
public enum BadgeType {
    TREND_NEW,
    POTENTIAL_NEW,
    TREND_REENTER,
    POTENTIAL_REENTER,
    RISING,
    STEADY
}
