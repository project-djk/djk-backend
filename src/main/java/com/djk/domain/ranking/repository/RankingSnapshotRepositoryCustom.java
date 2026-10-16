package com.djk.domain.ranking.repository;

import com.djk.domain.ranking.dto.SnapshotTimes;
import com.djk.domain.ranking.entity.BadgeType;
import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** QueryDSL 로 작성하는 조회 (ADR-0005). */
public interface RankingSnapshotRepositoryCustom {

    /** 한 랭킹 유형의 최신 스냅샷(snapshot_at 최댓값) row 를 순위 순서로, 키워드까지 SQL 한 번에 가져온다. */
    List<RankingSnapshot> findLatestSnapshotRows(RankingType rankingType, int limit);

    /** 랭킹 유형별 최신 snapshot_at 을 SQL 한 번에 가져온다. */
    SnapshotTimes findLatestSnapshotTimes();

    /** 같은 유형에서 before 바로 이전 스냅샷의 snapshot_at. 없으면 null. */
    LocalDateTime findPreviousSnapshotTime(RankingType rankingType, LocalDateTime before);

    /** 한 스냅샷(유형 + snapshot_at)에서 키워드별 순위. 그 스냅샷에 없는 키워드는 키가 없다. */
    Map<Long, Integer> findRankPositions(RankingType rankingType, LocalDateTime snapshotAt, Collection<Long> keywordIds);

    /** 스냅샷 row 들에 붙은 뱃지를 키워드별로 중복 없이 모은다. 뱃지가 없는 키워드는 키가 없다. */
    Map<Long, Set<BadgeType>> findBadgeTypesBySnapshotIds(Collection<Long> rankingSnapshotIds);
}
