package com.djk.domain.ranking.repository;

import com.djk.domain.ranking.dto.LatestSnapshotTimes;
import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import java.util.List;

/** QueryDSL 로 작성하는 조회 (ADR-0005). */
public interface RankingSnapshotRepositoryCustom {

    /** 한 랭킹 유형의 최신 스냅샷(snapshot_at 최댓값) row 를 순위 순서로, 키워드까지 SQL 한 번에 가져온다. */
    List<RankingSnapshot> findLatestSnapshotRows(RankingType rankingType, int limit);

    /** 랭킹 유형별 최신 snapshot_at 을 SQL 한 번에 가져온다. */
    LatestSnapshotTimes findLatestSnapshotTimes();
}
