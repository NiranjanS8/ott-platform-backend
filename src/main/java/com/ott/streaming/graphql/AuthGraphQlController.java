package com.ott.streaming.graphql;

import com.ott.streaming.dto.auth.AuthResponse;
import com.ott.streaming.dto.auth.AuthUser;
import com.ott.streaming.dto.auth.LoginInput;
import com.ott.streaming.dto.auth.RegisterInput;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class AuthGraphQlController {

    @MutationMapping
    public AuthResponse register(@Argument @Valid RegisterInput input) {
        throw new UnsupportedOperationException("Register mutation is not implemented yet");
    }

    @MutationMapping
    public AuthResponse login(@Argument @Valid LoginInput input) {
        throw new UnsupportedOperationException("Login mutation is not implemented yet");
    }

    @QueryMapping
    public AuthUser me() {
        throw new UnsupportedOperationException("Me query is not implemented yet");
    }
}
