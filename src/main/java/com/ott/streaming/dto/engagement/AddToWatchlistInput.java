package com.ott.streaming.dto.engagement;

import com.ott.streaming.entity.ContentType;
import jakarta.validation.constraints.NotNull;

public record AddToWatchlistInput(
        @NotNull(message = "Content type is required")
        ContentType contentType,
        @NotNull(message = "Content id is required")
        Long contentId
) {
}
