package com.ott.streaming.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        Throwable resolved = unwrap(ex);

        if (resolved instanceof ApiException apiException) {
            return buildError(env, apiException.getErrorType(), apiException.getMessage());
        }

        if (resolved instanceof ConstraintViolationException constraintViolationException) {
            String message = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .distinct()
                    .collect(Collectors.joining(", "));

            return buildError(env, ErrorType.BAD_REQUEST, "Validation failed: " + message);
        }

        if (resolved instanceof IllegalArgumentException illegalArgumentException) {
            return buildError(env, ErrorType.BAD_REQUEST, illegalArgumentException.getMessage());
        }

        if (resolved instanceof DataIntegrityViolationException dataIntegrityViolationException) {
            return buildError(
                    env,
                    ErrorType.BAD_REQUEST,
                    mapDataIntegrityMessage(dataIntegrityViolationException)
            );
        }

        if (resolved instanceof AccessDeniedException) {
            return buildError(env, ErrorType.FORBIDDEN, "Forbidden");
        }

        return buildError(env, ErrorType.INTERNAL_ERROR, "Unexpected server error");
    }

    private GraphQLError buildError(DataFetchingEnvironment env, ErrorType errorType, String message) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(errorType)
                .message(message)
                .extensions(Map.of("code", errorType.name()))
                .build();
    }

    private String mapDataIntegrityMessage(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (normalized.contains("unique")
                || normalized.contains("duplicate")
                || normalized.contains("uk_")) {
            return "Duplicate resource";
        }

        return "Request violates data integrity constraints";
    }

    private Throwable unwrap(Throwable ex) {
        Throwable current = ex;

        while (current != null && shouldUnwrap(current) && current.getCause() != null) {
            current = current.getCause();
        }

        return current == null ? ex : current;
    }

    private boolean shouldUnwrap(Throwable ex) {
        return ex instanceof CompletionException
                || ex instanceof ExecutionException
                || ex instanceof InvocationTargetException
                || ex instanceof java.lang.reflect.UndeclaredThrowableException;
    }
}
