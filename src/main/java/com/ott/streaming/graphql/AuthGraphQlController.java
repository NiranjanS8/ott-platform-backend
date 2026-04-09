package com.ott.streaming.graphql;

import com.ott.streaming.dto.auth.AuthResponse;
import com.ott.streaming.dto.auth.AuthUser;
import com.ott.streaming.dto.auth.LoginInput;
import com.ott.streaming.dto.auth.RegisterInput;
import com.ott.streaming.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class AuthGraphQlController {

    private final AuthService authService;

    public AuthGraphQlController(AuthService authService) {
        this.authService = authService;
    }

    @MutationMapping
    public AuthResponse register(@Argument @Valid RegisterInput input) {
        return authService.register(input);
    }

    @MutationMapping
    public AuthResponse login(@Argument @Valid LoginInput input) {
        return authService.login(input);
    }

    @QueryMapping
    public AuthUser me() {
        throw new UnsupportedOperationException("Me query is not implemented yet");
    }
}
