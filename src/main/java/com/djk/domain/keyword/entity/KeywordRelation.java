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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기준 키워드(keyword)에서 본 연관 키워드(relatedKeyword)와 연관도. 방향이 있다. */
@Entity
@Table(name = "KEYWORD_RELATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_keyword_id", nullable = false)
    private Keyword relatedKeyword;

    /** 0~1. */
    @Column(name = "relevance_score", nullable = false, precision = 6, scale = 4)
    private BigDecimal relevanceScore;
}
