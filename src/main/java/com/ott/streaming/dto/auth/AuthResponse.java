package com.ott.streaming.dto.auth;

public record AuthResponse(
        String accessToken,
        AuthUser user
) {
}
