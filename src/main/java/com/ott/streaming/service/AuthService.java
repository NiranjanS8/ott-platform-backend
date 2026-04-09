package com.ott.streaming.service;

import com.ott.streaming.dto.auth.AuthResponse;
import com.ott.streaming.dto.auth.AuthUser;
import com.ott.streaming.dto.auth.LoginInput;
import com.ott.streaming.dto.auth.RegisterInput;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.security.JwtService;
import java.util.Locale;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthValidationService authValidationService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthValidationService authValidationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authValidationService = authValidationService;
    }

    public AuthResponse register(RegisterInput input) {
        RegisterInput normalizedInput = normalizeRegisterInput(input);
        authValidationService.validateUniqueEmail(normalizedInput);

        User user = new User();
        user.setName(normalizedInput.name());
        user.setEmail(normalizedInput.email());
        user.setPassword(passwordEncoder.encode(normalizedInput.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginInput input) {
        String normalizedEmail = normalizeEmail(input.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException("Invalid email or password", ErrorType.UNAUTHORIZED));

        if (!passwordEncoder.matches(input.password(), user.getPassword())) {
            throw new ApiException("Invalid email or password", ErrorType.UNAUTHORIZED);
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                toAuthUser(user)
        );
    }

    private AuthUser toAuthUser(User user) {
        return new AuthUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private RegisterInput normalizeRegisterInput(RegisterInput input) {
        return new RegisterInput(
                input.name().trim(),
                normalizeEmail(input.email()),
                input.password()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
