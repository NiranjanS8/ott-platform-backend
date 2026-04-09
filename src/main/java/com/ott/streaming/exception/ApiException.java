package com.ott.streaming.exception;

import org.springframework.graphql.execution.ErrorType;

public class ApiException extends RuntimeException {

    private final ErrorType errorType;

    public ApiException(String message) {
        this(message, ErrorType.BAD_REQUEST);
    }

    public ApiException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
