package com.ott.streaming.dto.content;

import java.time.Instant;

public record GenrePayload(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
