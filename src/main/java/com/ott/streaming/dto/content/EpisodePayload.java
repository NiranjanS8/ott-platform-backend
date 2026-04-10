package com.ott.streaming.dto.content;

import java.time.Instant;

public record EpisodePayload(
        Long id,
        Long seasonId,
        String title,
        Integer episodeNumber,
        String description,
        Integer durationMinutes,
        String releaseDate,
        Instant createdAt,
        Instant updatedAt
) {
}
