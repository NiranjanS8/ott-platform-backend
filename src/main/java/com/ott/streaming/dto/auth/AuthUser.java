package com.ott.streaming.dto.auth;

import com.ott.streaming.entity.Role;
import java.time.Instant;

public record AuthUser(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
}
