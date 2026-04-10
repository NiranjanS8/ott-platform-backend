package com.ott.streaming.dto.content;

import java.time.Instant;

public record SeasonPayload(
        Long id,
        Long seriesId,
        String title,
        Integer seasonNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
