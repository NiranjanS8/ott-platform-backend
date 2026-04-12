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

    public static ApiException validation(String message) {
        return new ApiException(message, ErrorType.BAD_REQUEST);
    }

    public static ApiException duplicateResource(String message) {
        return new ApiException(message, ErrorType.BAD_REQUEST);
    }

    public static ApiException notFound(String message) {
        return new ApiException(message, ErrorType.NOT_FOUND);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(message, ErrorType.UNAUTHORIZED);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(message, ErrorType.FORBIDDEN);
    }
}
