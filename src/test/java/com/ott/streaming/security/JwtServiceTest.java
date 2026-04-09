package com.ott.streaming.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ott.streaming.config.properties.JwtProperties;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "ott-streaming-test",
                "Y2hhbmdlLW1lLWJlZm9yZS1hdXRoLWNoYW5nZS1tZS1iZWZvcmUtYXV0aA==",
                Duration.ofHours(1)
        );
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generatesTokenAndExtractsUsername() {
        User user = buildUser("alice@example.com", Role.USER);

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void validatesTokenAgainstMatchingUserDetails() {
        User user = buildUser("admin@example.com", Role.ADMIN);
        String token = jwtService.generateAccessToken(user);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin@example.com")
                .password("encoded")
                .roles("ADMIN")
                .build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void rejectsTokenForDifferentUserDetails() {
        User user = buildUser("owner@example.com", Role.ADMIN);
        String token = jwtService.generateAccessToken(user);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("viewer@example.com")
                .password("encoded")
                .roles("USER")
                .build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    private User buildUser(String email, Role role) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        return user;
    }
}
