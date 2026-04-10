package com.ott.streaming.dto.content;

import java.time.Instant;

public record PersonPayload(
        Long id,
        String name,
        String biography,
        String profileImageUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
