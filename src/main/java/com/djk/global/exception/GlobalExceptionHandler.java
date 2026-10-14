package com.djk.global.exception;

import com.djk.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 예외를 API 명세의 공통 에러 응답(ApiResponse)으로 바꾼다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_PARAMETER = "잘못된 요청 파라미터입니다.";

    /** 파라미터 제약(@Min 등) 위반, 타입 변환 실패. */
    @ExceptionHandler({
            HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidParameter(Exception e) {
        return ApiResponse.fail(INVALID_PARAMETER);
    }
}
