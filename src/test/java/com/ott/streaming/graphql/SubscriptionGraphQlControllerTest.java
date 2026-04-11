package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.ott.streaming.exception.GraphQlExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(SubscriptionGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class SubscriptionGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void createSubscriptionPlanValidationRejectsBlankName() {
        graphQlTester.document("""
                mutation {
                  createSubscriptionPlan(input: {
                    name: ""
                    description: "Premium monthly plan"
                    price: 9.99
                    durationDays: 30
                    active: true
                  }) {
                    id
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage()).contains("Plan name is required");
                });
    }
}
