package com.djk.global.exception;

/**
 * 요청이 가리키는 키워드·카테고리 등이 없을 때. 404 로 내려간다.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
