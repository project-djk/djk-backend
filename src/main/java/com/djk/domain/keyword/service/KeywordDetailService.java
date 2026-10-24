package com.djk.domain.keyword.service;

import com.djk.domain.keyword.dto.KeywordDetail;
import com.djk.domain.keyword.dto.RankEntry;
import com.djk.domain.keyword.entity.Keyword;
import com.djk.domain.keyword.repository.KeywordRepository;
import com.djk.domain.ranking.dto.SnapshotTimes;
import com.djk.domain.ranking.entity.BadgeType;
import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import com.djk.domain.ranking.repository.RankingSnapshotRepository;
import com.djk.global.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 키워드 상세 조회. 랭킹 정보는 유형별 최신 스냅샷 기준이고, 변화는 같은 유형의 직전 스냅샷과 비교한다 (snapshot_at).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordDetailService {

    private final KeywordRepository keywordRepository;
    private final RankingSnapshotRepository rankingSnapshotRepository;

    public KeywordDetail getDetail(long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 키워드를 찾을 수 없습니다."));
        List<String> categories = keywordRepository.findCategoryNames(List.of(keywordId))
                .getOrDefault(keywordId, List.of());

        SnapshotTimes latest = rankingSnapshotRepository.findLatestSnapshotTimes();
        LocalDateTime snapshotAt = latest.of(RankingType.TREND);
        RankingSnapshot trend = findRow(RankingType.TREND, snapshotAt, keywordId);
        RankingSnapshot previousTrend = findPreviousRow(trend);
        RankingSnapshot potential = findRow(RankingType.POTENTIAL, latest.of(RankingType.POTENTIAL), keywordId);
        RankingSnapshot previousPotential = findPreviousRow(potential);
        RankingSnapshot rising = findRow(RankingType.RISING, latest.of(RankingType.RISING), keywordId);

        return new KeywordDetail(
                keyword,
                categories,
                badges(keywordId, trend, potential, rising),
                rankEntry(trend, previousTrend),
                rankEntry(potential, previousPotential),
                previousTrend == null ? null : previousTrend.getTrendScore(),
                keywordRepository.findTopRelation(keywordId),
                snapshotAt);
    }

    /** 최신 스냅샷 row 들에 붙은 뱃지. 어느 최신 스냅샷에도 없으면 빈 목록. */
    private List<BadgeType> badges(long keywordId, RankingSnapshot... latestRows) {
        List<Long> snapshotIds = Stream.of(latestRows).filter(Objects::nonNull).map(RankingSnapshot::getId).toList();
        if (snapshotIds.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rankingSnapshotRepository.findBadgeTypesBySnapshotIds(snapshotIds)
                .getOrDefault(keywordId, Set.of()));
    }

    /** 그 유형의 스냅샷이 없으면(snapshotAt 이 null) null. */
    private RankingSnapshot findRow(RankingType rankingType, LocalDateTime snapshotAt, long keywordId) {
        if (snapshotAt == null) {
            return null;
        }
        return rankingSnapshotRepository.findSnapshotRow(rankingType, snapshotAt, keywordId);
    }

    /** 같은 유형의 바로 이전 스냅샷에서 같은 키워드의 row. current 가 null 이거나 이전 스냅샷에 없으면 null. */
    private RankingSnapshot findPreviousRow(RankingSnapshot current) {
        if (current == null) {
            return null;
        }
        LocalDateTime previousAt =
                rankingSnapshotRepository.findPreviousSnapshotTime(current.getRankingType(), current.getSnapshotAt());
        return findRow(current.getRankingType(), previousAt, current.getKeyword().getId());
    }

    private RankEntry rankEntry(RankingSnapshot current, RankingSnapshot previous) {
        Map<Long, Integer> previousRanks = previous == null
                ? Map.of()
                : Map.of(previous.getKeyword().getId(), previous.getRankPosition());
        return RankEntry.of(current, previousRanks);
    }
}
