package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.auth.AuthResponse;
import com.ott.streaming.dto.auth.LoginInput;
import com.ott.streaming.dto.auth.RegisterInput;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthValidationService authValidationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authValidationService);
    }

    @Test
    void registerCreatesUserWithEncodedPasswordAndReturnsToken() {
        RegisterInput input = new RegisterInput("  Alice  ", "  Alice@Example.com ", "Str0ng!Pass");
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            savedUser.setRole(Role.USER);
            ReflectionTestUtils.setField(savedUser, "createdAt", Instant.parse("2026-04-09T12:00:00Z"));
            ReflectionTestUtils.setField(savedUser, "updatedAt", Instant.parse("2026-04-09T12:00:00Z"));
            return savedUser;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(input);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("Alice");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.user().role()).isEqualTo(Role.USER);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = existingUser();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Adm1n!Pass", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginInput(" Admin@Example.com ", "Adm1n!Pass"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(user.getId());
        assertThat(response.user().email()).isEqualTo("admin@example.com");
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginInput("missing@example.com", "Adm1n!Pass")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = existingUser();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginInput("admin@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password");
    }

    private User existingUser() {
        User user = new User();
        user.setId(9L);
        user.setName("Admin");
        user.setEmail("admin@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-04-09T12:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-04-09T12:00:00Z"));
        return user;
    }
}
