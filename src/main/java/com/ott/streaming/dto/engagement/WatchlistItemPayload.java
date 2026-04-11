package com.ott.streaming.dto.engagement;

import com.ott.streaming.entity.ContentType;
import java.time.Instant;

public record WatchlistItemPayload(
        Long id,
        Long userId,
        ContentType contentType,
        Long contentId,
        Instant createdAt,
        Instant updatedAt
) {
}
