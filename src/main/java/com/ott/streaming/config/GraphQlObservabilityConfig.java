package com.ott.streaming.config;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQlObservabilityConfig {

    @Bean
    GraphQlSourceBuilderCustomizer graphQlDepthLimitCustomizer(
            @Value("${app.graphql.max-query-depth:8}") int maxQueryDepth
    ) {
        return builder -> {
            if (maxQueryDepth > 0) {
                builder.instrumentation(List.of(maxQueryDepthInstrumentation(maxQueryDepth)));
            }
        };
    }

    private Instrumentation maxQueryDepthInstrumentation(int maxQueryDepth) {
        return new MaxQueryDepthInstrumentation(maxQueryDepth);
    }
}
