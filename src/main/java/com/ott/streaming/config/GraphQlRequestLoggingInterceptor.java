package com.ott.streaming.config;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GraphQlRequestLoggingInterceptor implements WebGraphQlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GraphQlRequestLoggingInterceptor.class);
    private static final int DOCUMENT_SNIPPET_LIMIT = 120;

    private final boolean logRequests;

    public GraphQlRequestLoggingInterceptor(@Value("${app.graphql.log-requests:true}") boolean logRequests) {
        this.logRequests = logRequests;
    }

    @Override
    public Mono<org.springframework.graphql.server.WebGraphQlResponse> intercept(
            WebGraphQlRequest request,
            Chain chain
    ) {
        if (!logRequests) {
            return chain.next(request);
        }

        long startedAt = System.nanoTime();
        log.info(
                "GraphQL request id={} operation={} type={} variables={} document=\"{}\"",
                request.getId(),
                operationName(request),
                operationType(request),
                request.getVariables().keySet(),
                documentSnippet(request)
        );

        return chain.next(request)
                .doOnNext(response -> log.info(
                        "GraphQL response id={} operation={} valid={} errors={} durationMs={}",
                        request.getId(),
                        operationName(request),
                        response.isValid(),
                        response.getErrors().size(),
                        elapsedMillis(startedAt)
                ))
                .doOnError(error -> log.warn(
                        "GraphQL request failed id={} operation={} durationMs={} message={}",
                        request.getId(),
                        operationName(request),
                        elapsedMillis(startedAt),
                        error.getMessage()
                ));
    }

    private String operationName(WebGraphQlRequest request) {
        String operationName = request.getOperationName();
        return operationName == null || operationName.isBlank() ? "anonymous" : operationName;
    }

    private String operationType(WebGraphQlRequest request) {
        String document = request.getDocument();
        if (document == null || document.isBlank()) {
            return "unknown";
        }

        String normalized = document.stripLeading().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("mutation")) {
            return "mutation";
        }
        if (normalized.startsWith("subscription")) {
            return "subscription";
        }
        return "query";
    }

    private String documentSnippet(WebGraphQlRequest request) {
        if (isSensitiveOperation(request)) {
            return "<redacted>";
        }

        String document = request.getDocument();
        if (document == null || document.isBlank()) {
            return "";
        }

        String normalized = document.replaceAll("\\s+", " ").trim();
        return normalized.length() <= DOCUMENT_SNIPPET_LIMIT
                ? normalized
                : normalized.substring(0, DOCUMENT_SNIPPET_LIMIT) + "...";
    }

    private boolean isSensitiveOperation(WebGraphQlRequest request) {
        String normalized = request.getDocument() == null ? "" : request.getDocument().toLowerCase(Locale.ROOT);
        String operationName = operationName(request).toLowerCase(Locale.ROOT);
        return normalized.contains("login(")
                || normalized.contains("register(")
                || "login".equals(operationName)
                || "register".equals(operationName);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
