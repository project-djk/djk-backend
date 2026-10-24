package com.djk.domain.keyword.dto;

import com.djk.domain.keyword.entity.Keyword;
import java.util.List;

/**
 * 키워드 상세 조회 결과. 점수는 DB 스케일(0~1) 그대로다.
 */
public record KeywordDetail(Keyword keyword, List<String> categories) {
}
