package com.ott.streaming.dto.engagement;

import com.ott.streaming.entity.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateWatchProgressInput(
        @NotNull(message = "Content type is required")
        ContentType contentType,
        @NotNull(message = "Content id is required")
        @Positive(message = "Content id must be greater than 0")
        Long contentId,
        @Positive(message = "Season id must be greater than 0")
        Long seasonId,
        @Positive(message = "Episode id must be greater than 0")
        Long episodeId,
        @NotNull(message = "Progress seconds are required")
        @Min(value = 0, message = "Progress seconds must be at least 0")
        Integer progressSeconds,
        @NotNull(message = "Duration seconds are required")
        @Min(value = 1, message = "Duration seconds must be greater than 0")
        Integer durationSeconds
) {
}
