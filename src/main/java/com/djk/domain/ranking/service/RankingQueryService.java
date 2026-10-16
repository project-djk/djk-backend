package com.djk.domain.ranking.service;

import com.djk.domain.ranking.dto.LatestRanking;
import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import com.djk.domain.ranking.repository.RankingSnapshotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
        List<RankingSnapshot> rows = rankingSnapshotRepository.findLatestSnapshotRows(rankingType, limit);
        if (rows.isEmpty()) {
            return LatestRanking.empty();
        }
        // 한 스냅샷의 row 는 모두 같은 snapshot_at 을 가진다.
        return new LatestRanking(rows.get(0).getSnapshotAt(), rows);
    }
}
