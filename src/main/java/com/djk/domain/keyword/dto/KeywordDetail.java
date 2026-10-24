package com.djk.domain.keyword.dto;

import com.djk.domain.keyword.entity.Keyword;
import com.djk.domain.keyword.entity.KeywordRelation;
import com.djk.domain.ranking.entity.BadgeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 키워드 상세 조회 결과. 점수는 DB 스케일(0~1) 그대로다.
 *
 * @param badges             최신 TREND·POTENTIAL·RISING 스냅샷 row 에 붙은 뱃지 (중복 없음, enum 순서)
 * @param trend              최신 TREND 스냅샷에 없으면 null
 * @param potential          최신 POTENTIAL 스냅샷에 없으면 null
 * @param previousTrendScore 직전 TREND 스냅샷에서의 트렌드 지수. trend 가 null 이거나 직전 스냅샷에 없었으면 null
 * @param topRelation        연관도가 가장 높은 연관 관계. 연관어가 없으면 null
 * @param snapshotAt         최신 TREND 스냅샷의 snapshot_at. TREND 스냅샷이 없으면 null
 */
public record KeywordDetail(
        Keyword keyword,
        List<String> categories,
        List<BadgeType> badges,
        RankEntry trend,
        RankEntry potential,
        BigDecimal previousTrendScore,
        KeywordRelation topRelation,
        LocalDateTime snapshotAt) {
}
