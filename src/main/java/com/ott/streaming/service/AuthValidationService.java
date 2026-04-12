package com.ott.streaming.service;

import com.ott.streaming.dto.auth.RegisterInput;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthValidationService {

    private final UserRepository userRepository;

    public AuthValidationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateUniqueEmail(RegisterInput input) {
        if (userRepository.existsByEmail(input.email())) {
            throw ApiException.duplicateResource("Email is already registered");
        }
    }
}
