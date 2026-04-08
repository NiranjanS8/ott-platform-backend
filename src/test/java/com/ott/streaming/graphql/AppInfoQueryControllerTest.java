package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.ott.streaming.config.properties.AppProperties;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(AppInfoQueryController.class)
@Import({AppInfoQueryControllerTest.TestConfig.class, GraphQlExceptionHandler.class})
class AppInfoQueryControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void returnsApplicationInfo() {
        graphQlTester.document("""
                query {
                  appInfo {
                    name
                    environment
                    graphqlPath
                  }
                }
                """)
                .execute()
                .path("appInfo.name").entity(String.class).satisfies(name ->
                        assertThat(name).isEqualTo("OTT Streaming Backend"))
                .path("appInfo.environment").entity(String.class).satisfies(environment ->
                        assertThat(environment).isEqualTo("local"))
                .path("appInfo.graphqlPath").entity(String.class).satisfies(path ->
                        assertThat(path).isEqualTo("/graphql"));
    }

    @Test
    void returnsHealthStatus() {
        graphQlTester.document("""
                query {
                  health
                }
                """)
                .execute()
                .path("health")
                .entity(String.class)
                .isEqualTo("UP");
    }

    @Test
    void returnsValidationFriendlyErrorForBlankPingMessage() {
        graphQlTester.document("""
                query {
                  ping(message: "   ")
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage()).contains("Message must not be blank");
                });
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AppProperties appProperties() {
            return new AppProperties("OTT Streaming Backend", "local");
        }
    }
}
