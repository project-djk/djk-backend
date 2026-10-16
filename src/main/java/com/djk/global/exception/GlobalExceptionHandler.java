package com.djk.global.exception;

import com.djk.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 예외를 API 명세의 공통 에러 응답(ApiResponse)으로 바꾼다.
 */
@Slf4j
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
        log.debug("잘못된 요청 파라미터: {}", e.getMessage());
        return ApiResponse.fail(INVALID_PARAMETER);
    }

    /** 파라미터 조합이 잘못됨. 명세에 따라 보낸 값을 details 로 돌려준다. */
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleInvalidParameterCombination(InvalidParameterException e) {
        return ApiResponse.fail(e.getMessage(), e.getDetails());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.fail(e.getMessage());
    }
}
