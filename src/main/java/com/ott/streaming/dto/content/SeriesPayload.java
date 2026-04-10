package com.ott.streaming.dto.content;

import java.time.Instant;

public record SeriesPayload(
        Long id,
        String title,
        String description,
        String releaseDate,
        String endDate,
        String maturityRating,
        Instant createdAt,
        Instant updatedAt
) {
}
