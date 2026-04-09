package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.auth.AuthResponse;
import com.ott.streaming.dto.auth.AuthUser;
import com.ott.streaming.entity.Role;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(AuthGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class AuthGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerMutationReturnsTokenAndUser() {
        when(authService.register(any())).thenReturn(sampleResponse("new-user@example.com", Role.USER));

        graphQlTester.document("""
                mutation {
                  register(input: {
                    name: "New User"
                    email: "new-user@example.com"
                    password: "Str0ng!Pass"
                  }) {
                    accessToken
                    user {
                      id
                      name
                      email
                      role
                    }
                  }
                }
                """)
                .execute()
                .path("register.accessToken").entity(String.class).isEqualTo("jwt-token")
                .path("register.user.email").entity(String.class).isEqualTo("new-user@example.com")
                .path("register.user.role").entity(String.class).isEqualTo("USER");
    }

    @Test
    void loginMutationReturnsTokenAndUser() {
        when(authService.login(any())).thenReturn(sampleResponse("admin@example.com", Role.ADMIN));

        graphQlTester.document("""
                mutation {
                  login(input: {
                    email: "admin@example.com"
                    password: "Adm1n!Pass"
                  }) {
                    accessToken
                    user {
                      email
                      role
                    }
                  }
                }
                """)
                .execute()
                .path("login.accessToken").entity(String.class).isEqualTo("jwt-token")
                .path("login.user.email").entity(String.class).isEqualTo("admin@example.com")
                .path("login.user.role").entity(String.class).isEqualTo("ADMIN");
    }

    @Test
    void registerMutationReturnsValidationErrorsForWeakPassword() {
        graphQlTester.document("""
                mutation {
                  register(input: {
                    name: ""
                    email: "bad-email"
                    password: "weak"
                  }) {
                    accessToken
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage())
                            .contains("Name is required")
                            .contains("Email must be valid")
                            .contains("Password must be at least 8 characters and include uppercase, lowercase, number, and special character");
                });
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void meQueryReturnsAuthenticatedUser() {
        when(authService.getCurrentUser("member@example.com")).thenReturn(sampleResponse("member@example.com", Role.USER).user());

        graphQlTester.document("""
                query {
                  me {
                    email
                    role
                  }
                }
                """)
                .execute()
                .path("me.email").entity(String.class).isEqualTo("member@example.com")
                .path("me.role").entity(String.class).isEqualTo("USER");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminStatusQueryReturnsValueForAdmin() {
        when(authService.adminStatus()).thenReturn("ADMIN_ACCESS_GRANTED");

        graphQlTester.document("""
                query {
                  adminStatus
                }
                """)
                .execute()
                .path("adminStatus").entity(String.class).isEqualTo("ADMIN_ACCESS_GRANTED");
    }

    private AuthResponse sampleResponse(String email, Role role) {
        return new AuthResponse(
                "jwt-token",
                new AuthUser(
                        1L,
                        role == Role.ADMIN ? "Admin" : "New User",
                        email,
                        role,
                        Instant.parse("2026-04-09T12:00:00Z"),
                        Instant.parse("2026-04-09T12:00:00Z")
                )
        );
    }
}
