package com.djk.domain.ranking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 랭킹 스냅샷 row 에 부여된 뱃지. 한 row 가 여러 뱃지를 가질 수 있다. */
@Entity
@Table(name = "RANKING_SNAPSHOT_BADGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingSnapshotBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ranking_snapshot_id", nullable = false)
    private RankingSnapshot rankingSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false)
    private BadgeType badgeType;
}
