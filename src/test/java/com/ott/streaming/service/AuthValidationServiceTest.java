package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.auth.RegisterInput;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthValidationService authValidationService;

    @BeforeEach
    void setUp() {
        authValidationService = new AuthValidationService(userRepository);
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        RegisterInput input = new RegisterInput("Alice", "alice@example.com", "Str0ng!Pass");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authValidationService.validateUniqueEmail(input))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email is already registered");
    }
}
