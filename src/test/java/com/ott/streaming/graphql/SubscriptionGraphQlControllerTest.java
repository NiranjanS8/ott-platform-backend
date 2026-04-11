package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.subscription.SubscriptionPlanPayload;
import com.ott.streaming.dto.subscription.UserSubscriptionPayload;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.SubscriptionStatus;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.SubscriptionAdminService;
import com.ott.streaming.service.UserSubscriptionService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(SubscriptionGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class SubscriptionGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private SubscriptionAdminService subscriptionAdminService;

    @MockitoBean
    private UserSubscriptionService userSubscriptionService;

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

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void createSubscriptionPlanReturnsPayloadForAdmin() {
        when(subscriptionAdminService.createSubscriptionPlan(any())).thenReturn(
                new SubscriptionPlanPayload(
                        1L,
                        "Premium Monthly",
                        "Access for 30 days",
                        new BigDecimal("9.99"),
                        30,
                        true,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  createSubscriptionPlan(input: {
                    name: "Premium Monthly"
                    description: "Access for 30 days"
                    price: 9.99
                    durationDays: 30
                    active: true
                  }) {
                    id
                    name
                    durationDays
                  }
                }
                """)
                .execute()
                .path("createSubscriptionPlan.id").entity(String.class).isEqualTo("1")
                .path("createSubscriptionPlan.name").entity(String.class).isEqualTo("Premium Monthly")
                .path("createSubscriptionPlan.durationDays").entity(Integer.class).isEqualTo(30);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateMovieAccessLevelReturnsUpdatedMovie() {
        when(subscriptionAdminService.updateMovieAccessLevel(any(), any())).thenReturn(
                new MoviePayload(
                        10L,
                        "The Matrix",
                        "Sci-fi action film",
                        "1999-03-31",
                        136,
                        "R",
                        ContentAccessLevel.PREMIUM,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  updateMovieAccessLevel(id: "10", input: { accessLevel: PREMIUM }) {
                    id
                    title
                    accessLevel
                  }
                }
                """)
                .execute()
                .path("updateMovieAccessLevel.id").entity(String.class).isEqualTo("10")
                .path("updateMovieAccessLevel.title").entity(String.class).isEqualTo("The Matrix")
                .path("updateMovieAccessLevel.accessLevel").entity(String.class).isEqualTo("PREMIUM");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void subscribeToPlanReturnsPayloadForAuthenticatedUser() {
        when(userSubscriptionService.subscribeToPlan(any(), any())).thenReturn(
                new UserSubscriptionPayload(
                        2L,
                        5L,
                        1L,
                        SubscriptionStatus.ACTIVE,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-05-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  subscribeToPlan(input: { planId: "1" }) {
                    id
                    userId
                    planId
                    status
                  }
                }
                """)
                .execute()
                .path("subscribeToPlan.id").entity(String.class).isEqualTo("2")
                .path("subscribeToPlan.userId").entity(String.class).isEqualTo("5")
                .path("subscribeToPlan.planId").entity(String.class).isEqualTo("1")
                .path("subscribeToPlan.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void currentSubscriptionReturnsPayloadForAuthenticatedUser() {
        when(userSubscriptionService.getCurrentSubscription("member@example.com")).thenReturn(
                new UserSubscriptionPayload(
                        3L,
                        5L,
                        1L,
                        SubscriptionStatus.ACTIVE,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-05-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                query {
                  currentSubscription {
                    id
                    status
                    planId
                  }
                }
                """)
                .execute()
                .path("currentSubscription.id").entity(String.class).isEqualTo("3")
                .path("currentSubscription.status").entity(String.class).isEqualTo("ACTIVE")
                .path("currentSubscription.planId").entity(String.class).isEqualTo("1");
    }
}
