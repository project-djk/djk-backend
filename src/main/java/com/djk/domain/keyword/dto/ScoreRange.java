package com.djk.domain.keyword.dto;

import com.djk.global.exception.InvalidParameterException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 스케일(0~100, 정수)로 받은 점수 범위. 양 끝값을 포함한다.
 */
public record ScoreRange(int min, int max) {

    public static final int LOWEST = 0;
    public static final int HIGHEST = 100;

    /**
     * @param name 에러 메시지와 details 에 쓰는 파라미터 이름 접두사 (예: "trend" → trendMin / trendMax)
     */
    public static ScoreRange of(String name, int min, int max) {
        if (min > max) {
            Map<String, Integer> details = new LinkedHashMap<>();
            details.put(name + "Min", min);
            details.put(name + "Max", max);
            throw new InvalidParameterException(
                    "%sMin은 %sMax보다 클 수 없습니다.".formatted(name, name), details);
        }
        return new ScoreRange(min, max);
    }

    /** 0~100 전체면 필터를 걸지 않은 것으로 본다. 그 지수가 없는 키워드도 포함한다. */
    public boolean isUnbounded() {
        return min == LOWEST && max == HIGHEST;
    }

    /** DB 스케일(0~1) 하한. */
    public BigDecimal lowerScore() {
        return BigDecimal.valueOf(min, 2);
    }

    /** DB 스케일(0~1) 상한. */
    public BigDecimal upperScore() {
        return BigDecimal.valueOf(max, 2);
    }
}
