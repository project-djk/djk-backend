package com.djk.domain.keyword.dto;

import com.djk.domain.keyword.entity.Keyword;
import com.djk.domain.ranking.entity.BadgeType;
import java.util.List;

/**
 * 검색 결과 항목 하나. 점수는 DB 스케일(0~1) 그대로다.
 *
 * @param trend     최신 TREND 스냅샷에 없으면 null
 * @param potential 최신 POTENTIAL 스냅샷에 없으면 null
 */
public record KeywordSearchHit(Keyword keyword, List<BadgeType> badges, RankEntry trend, RankEntry potential) {
}
