package com.ott.streaming.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ott.streaming.dto.auth.LoginInput;
import com.ott.streaming.dto.auth.RegisterInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthInputValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void registerInputRejectsMissingFieldsAndWeakPassword() {
        RegisterInput input = new RegisterInput("", "invalid-email", "weak");

        Set<ConstraintViolation<RegisterInput>> violations = validator.validate(input);

        assertThat(messagesOf(violations))
                .contains("Name is required")
                .contains("Email must be valid")
                .contains("Password must be at least 8 characters and include uppercase, lowercase, number, and special character");
    }

    @Test
    void registerInputAcceptsStrongPasswordAndRequiredFields() {
        RegisterInput input = new RegisterInput("Alice", "alice@example.com", "Str0ng!Pass");

        Set<ConstraintViolation<RegisterInput>> violations = validator.validate(input);

        assertThat(violations).isEmpty();
    }

    @Test
    void loginInputRequiresEmailAndPassword() {
        LoginInput input = new LoginInput("", "");

        Set<ConstraintViolation<LoginInput>> violations = validator.validate(input);

        assertThat(messagesOf(violations))
                .contains("Email is required")
                .contains("Password is required");
    }

    private Set<String> messagesOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
