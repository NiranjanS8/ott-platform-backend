package com.ott.streaming.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.Map;
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
        if (ex instanceof ApiException apiException) {
            return buildError(env, apiException.getErrorType(), apiException.getMessage());
        }

        if (ex instanceof ConstraintViolationException constraintViolationException) {
            String message = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .distinct()
                    .collect(Collectors.joining(", "));

            return buildError(env, ErrorType.BAD_REQUEST, "Validation failed: " + message);
        }

        if (ex instanceof IllegalArgumentException illegalArgumentException) {
            return buildError(env, ErrorType.BAD_REQUEST, illegalArgumentException.getMessage());
        }

        if (ex instanceof DataIntegrityViolationException dataIntegrityViolationException) {
            return buildError(
                    env,
                    ErrorType.BAD_REQUEST,
                    mapDataIntegrityMessage(dataIntegrityViolationException)
            );
        }

        if (ex instanceof AccessDeniedException) {
            return buildError(env, ErrorType.FORBIDDEN, "Access Denied");
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
}
