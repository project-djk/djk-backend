package com.djk.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 모든 API가 공통으로 사용하는 응답 래퍼.
 * JSON 필드명은 API 명세 기준인 isSuccess / message / details 를 따른다.
 */
public final class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T details;

    private ApiResponse(boolean success, String message, T details) {
        this.success = success;
        this.message = message;
        this.details = details;
    }

    public static <T> ApiResponse<T> success(String message, T details) {
        return new ApiResponse<>(true, message, details);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }

    // getter 이름(isSuccess)만으로는 JSON 키가 "success"가 되므로 명시적으로 지정한다.
    @JsonProperty("isSuccess")
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getDetails() {
        return details;
    }
}
