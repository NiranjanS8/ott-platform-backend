package com.ott.streaming.dto.engagement;

import com.ott.streaming.entity.ContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToWatchlistInput(
        @NotNull(message = "Content type is required")
        ContentType contentType,
        @NotNull(message = "Content id is required")
        @Positive(message = "Content id must be greater than 0")
        Long contentId
) {
}
