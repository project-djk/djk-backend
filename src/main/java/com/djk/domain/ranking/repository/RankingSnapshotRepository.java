package com.djk.domain.ranking.repository;

import com.djk.domain.ranking.entity.RankingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingSnapshotRepository
        extends JpaRepository<RankingSnapshot, Long>, RankingSnapshotRepositoryCustom {
}
