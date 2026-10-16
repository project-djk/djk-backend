package com.djk.domain.keyword.dto;

import java.util.Arrays;

/**
 * 상세 검색 trendLine(스파크라인)의 기간. API 값은 "7D" / "30D" / "90D" 다.
 */
public enum TrendPeriod {
    DAYS_7("7D", 7),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90);

    /** 컨트롤러 파라미터 검증용. 값 목록과 같아야 한다. */
    public static final String CODE_PATTERN = "7D|30D|90D";

    private final String code;
    private final int days;

    TrendPeriod(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public int days() {
        return days;
    }

    public static TrendPeriod fromCode(String code) {
        return Arrays.stream(values())
                .filter(period -> period.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 period: " + code));
    }
}
