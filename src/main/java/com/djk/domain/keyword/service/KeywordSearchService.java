package com.djk.domain.keyword.service;

import com.djk.domain.category.repository.CategoryRepository;
import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchHit;
import com.djk.domain.keyword.dto.KeywordSearchResult;
import com.djk.domain.keyword.dto.KeywordSearchRow;
import com.djk.domain.keyword.dto.RankEntry;
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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 키워드 상세 검색. 검색 대상은 최신 TREND·POTENTIAL·RISING 스냅샷에 든 키워드다 (CONTEXT.md "검색 대상 키워드").
 * 조건에 맞는 키워드 한 페이지를 먼저 뽑고, 그 키워드들의 상세 정보는 묶어서 따로 조회한다 (N+1 없음).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordSearchService {

    private final KeywordRepository keywordRepository;
    private final RankingSnapshotRepository rankingSnapshotRepository;
    private final CategoryRepository categoryRepository;

    public KeywordSearchResult search(KeywordSearchCondition condition) {
        if (condition.categoryId() != null && !categoryRepository.existsById(condition.categoryId())) {
            throw new ResourceNotFoundException("해당 카테고리를 찾을 수 없습니다.");
        }
        SnapshotTimes latest = rankingSnapshotRepository.findLatestSnapshotTimes();
        if (latest.isEmpty()) {
            return KeywordSearchResult.empty(null, condition);
        }
        LocalDateTime snapshotAt = latest.of(RankingType.TREND);
        List<KeywordSearchRow> rows = keywordRepository.search(condition, latest);
        long total = keywordRepository.countSearch(condition, latest);
        return new KeywordSearchResult(
                snapshotAt, condition.page(), condition.size(), total, toHits(rows, latest));
    }

    private List<KeywordSearchHit> toHits(List<KeywordSearchRow> rows, SnapshotTimes latest) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> keywordIds = rows.stream().map(row -> row.keyword().getId()).toList();
        List<Long> snapshotIds = rows.stream().flatMap(KeywordSearchRow::snapshots).map(RankingSnapshot::getId).toList();
        Map<Long, Set<BadgeType>> badges = rankingSnapshotRepository.findBadgeTypesBySnapshotIds(snapshotIds);
        Map<Long, Integer> previousTrendRanks = previousRanks(RankingType.TREND, latest, keywordIds);
        Map<Long, Integer> previousPotentialRanks = previousRanks(RankingType.POTENTIAL, latest, keywordIds);

        return rows.stream()
                .map(row -> new KeywordSearchHit(
                        row.keyword(),
                        List.copyOf(badges.getOrDefault(row.keyword().getId(), Set.of())),
                        RankEntry.of(row.trend(), previousTrendRanks),
                        RankEntry.of(row.potential(), previousPotentialRanks)))
                .toList();
    }

    /** 최신 스냅샷 바로 이전 스냅샷에서의 키워드별 순위. 이전 스냅샷이 없으면 비어 있다. */
    private Map<Long, Integer> previousRanks(RankingType rankingType, SnapshotTimes latest, List<Long> keywordIds) {
        LocalDateTime latestAt = latest.of(rankingType);
        if (latestAt == null) {
            return Map.of();
        }
        LocalDateTime previousAt = rankingSnapshotRepository.findPreviousSnapshotTime(rankingType, latestAt);
        if (previousAt == null) {
            return Map.of();
        }
        return rankingSnapshotRepository.findRankPositions(rankingType, previousAt, keywordIds);
    }
}
