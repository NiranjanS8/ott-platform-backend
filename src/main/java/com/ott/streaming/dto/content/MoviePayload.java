package com.ott.streaming.dto.content;

import com.ott.streaming.entity.ContentAccessLevel;
import java.time.Instant;

public record MoviePayload(
        Long id,
        String title,
        String description,
        String releaseDate,
        Integer durationMinutes,
        String maturityRating,
        String language,
        ContentAccessLevel accessLevel,
        Instant createdAt,
        Instant updatedAt
) {
}
