package com.djk.domain.ranking.entity;

import com.djk.domain.keyword.entity.Keyword;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 번의 집계(iteration)에서 저장된 랭킹 row. 같은 집계의 row 는 모두 같은 snapshotAt 을 가진다.
 */
@Entity
@Table(name = "RANKING_SNAPSHOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false)
    private RankingType rankingType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    /** 0~1 정규화 값. 100점 환산은 응답 DTO 에서만 한다. */
    @Column(name = "trend_score", nullable = false, precision = 6, scale = 4)
    private BigDecimal trendScore;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;
}
