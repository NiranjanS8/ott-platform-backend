package com.ott.streaming.dto.review;

import com.ott.streaming.entity.ContentType;
import java.time.Instant;

public record ReviewPayload(
        Long id,
        Long userId,
        ContentType contentType,
        Long contentId,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
