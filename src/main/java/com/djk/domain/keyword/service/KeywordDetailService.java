package com.djk.domain.keyword.service;

import com.djk.domain.keyword.dto.KeywordDetail;
import com.djk.domain.keyword.entity.Keyword;
import com.djk.domain.keyword.repository.KeywordRepository;
import com.djk.global.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 키워드 상세 조회. 랭킹 정보는 유형별 최신 스냅샷 기준이다 (snapshot_at).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordDetailService {

    private final KeywordRepository keywordRepository;

    public KeywordDetail getDetail(long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 키워드를 찾을 수 없습니다."));
        List<String> categories = keywordRepository.findCategoryNames(List.of(keywordId))
                .getOrDefault(keywordId, List.of());
        return new KeywordDetail(keyword, categories);
    }
}
