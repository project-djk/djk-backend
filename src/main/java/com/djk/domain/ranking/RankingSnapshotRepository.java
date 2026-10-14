package com.djk.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshot, Long> {

    @Query("SELECT MAX(r.snapshotAt) FROM RankingSnapshot r WHERE r.rankingType = :rankingType")
    Optional<LocalDateTime> findLatestSnapshotAt(RankingType rankingType);

    @Query("""
            SELECT r FROM RankingSnapshot r JOIN FETCH r.keyword
            WHERE r.rankingType = :rankingType AND r.snapshotAt = :snapshotAt
            ORDER BY r.rankPosition
            """)
    List<RankingSnapshot> findRanking(RankingType rankingType, LocalDateTime snapshotAt, Limit limit);
}
