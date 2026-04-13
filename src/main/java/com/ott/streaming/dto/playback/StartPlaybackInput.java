package com.ott.streaming.dto.playback;

import com.ott.streaming.entity.ContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartPlaybackInput(
        @NotNull(message = "Content type is required")
        ContentType contentType,
        @NotNull(message = "Content id is required")
        @Positive(message = "Content id must be positive")
        Long contentId,
        @Positive(message = "Season id must be positive")
        Long seasonId,
        @Positive(message = "Episode id must be positive")
        Long episodeId
) {
}
