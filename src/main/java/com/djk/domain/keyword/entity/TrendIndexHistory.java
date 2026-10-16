package com.djk.domain.keyword.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 키워드별 트렌드 지수 시계열의 한 점. */
@Entity
@Table(name = "TREND_INDEX_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrendIndexHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    /** 0~1 정규화 값. 100점 환산은 응답 DTO 에서만 한다. */
    @Column(name = "trend_score", nullable = false, precision = 6, scale = 4)
    private BigDecimal trendScore;
}
