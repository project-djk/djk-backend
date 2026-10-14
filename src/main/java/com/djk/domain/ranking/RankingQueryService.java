package com.djk.domain.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 랭킹 조회. "최신"은 항상 snapshot_at 기준이다 (created_at 은 쓰지 않는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingQueryService {

    private final RankingSnapshotRepository rankingSnapshotRepository;

    public LatestRanking getLatestTop(RankingType rankingType, int limit) {
        return rankingSnapshotRepository.findLatestSnapshotAt(rankingType)
                .map(snapshotAt -> new LatestRanking(
                        snapshotAt,
                        rankingSnapshotRepository.findRanking(rankingType, snapshotAt, Limit.of(limit))))
                .orElseGet(LatestRanking::empty);
    }
}
