package com.djk.global.exception;

import lombok.Getter;

/**
 * 파라미터 하나하나는 유효하지만 조합이 잘못됐을 때(예: 하한 > 상한). 400 과 함께 details 를 내려준다.
 */
@Getter
public class InvalidParameterException extends RuntimeException {

    private final transient Object details;

    public InvalidParameterException(String message, Object details) {
        super(message);
        this.details = details;
    }
}
