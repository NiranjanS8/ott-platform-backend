package com.ott.streaming.dto.engagement;

import com.ott.streaming.entity.ContentType;
import java.time.Instant;

public record WatchProgressPayload(
        Long id,
        Long userId,
        ContentType contentType,
        Long contentId,
        Long seasonId,
        Long episodeId,
        Integer progressSeconds,
        Integer durationSeconds,
        boolean completed,
        Instant lastWatchedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
