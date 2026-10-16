package com.djk.domain.keyword.dto;

import com.djk.domain.keyword.entity.Keyword;
import com.djk.domain.ranking.entity.RankingSnapshot;

/**
 * 검색 조건에 맞은 키워드 하나와, 그 키워드의 최신 TREND·POTENTIAL 스냅샷 row. 해당 랭킹에 없으면 null.
 */
public record KeywordSearchRow(Keyword keyword, RankingSnapshot trend, RankingSnapshot potential) {
}
