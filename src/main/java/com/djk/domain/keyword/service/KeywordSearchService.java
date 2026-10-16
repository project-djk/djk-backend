package com.djk.domain.keyword.service;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchResult;
import com.djk.domain.keyword.dto.KeywordSearchRow;
import com.djk.domain.keyword.repository.KeywordRepository;
import com.djk.domain.ranking.dto.LatestSnapshotTimes;
import com.djk.domain.ranking.entity.RankingType;
import com.djk.domain.ranking.repository.RankingSnapshotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 키워드 상세 검색. 검색 대상은 최신 TREND·POTENTIAL·RISING 스냅샷에 든 키워드다 (CONTEXT.md "검색 대상 키워드").
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordSearchService {

    private final KeywordRepository keywordRepository;
    private final RankingSnapshotRepository rankingSnapshotRepository;

    public KeywordSearchResult search(KeywordSearchCondition condition) {
        LatestSnapshotTimes latest = rankingSnapshotRepository.findLatestSnapshotTimes();
        if (latest.isEmpty()) {
            return KeywordSearchResult.empty(null, condition);
        }
        List<KeywordSearchRow> rows = keywordRepository.search(condition, latest);
        long total = keywordRepository.countSearch(condition, latest);
        return new KeywordSearchResult(latest.of(RankingType.TREND), condition.page(), condition.size(), total, rows);
    }
}
