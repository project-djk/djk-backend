package com.djk.domain.ranking.repository;

import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshot, Long> {

    /** 한 랭킹 유형의 최신 스냅샷(snapshot_at 최댓값) row 를 순위 순서로, 키워드까지 SQL 한 번에 가져온다. */
    @Query("""
            SELECT r FROM RankingSnapshot r JOIN FETCH r.keyword
            WHERE r.rankingType = :rankingType
              AND r.snapshotAt = (SELECT MAX(s.snapshotAt) FROM RankingSnapshot s WHERE s.rankingType = :rankingType)
            ORDER BY r.rankPosition
            """)
    List<RankingSnapshot> findLatestSnapshotRows(RankingType rankingType, Limit limit);
}
