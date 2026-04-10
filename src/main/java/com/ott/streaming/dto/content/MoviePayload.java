package com.ott.streaming.dto.content;

import java.time.Instant;

public record MoviePayload(
        Long id,
        String title,
        String description,
        String releaseDate,
        Integer durationMinutes,
        String maturityRating,
        Instant createdAt,
        Instant updatedAt
) {
}
