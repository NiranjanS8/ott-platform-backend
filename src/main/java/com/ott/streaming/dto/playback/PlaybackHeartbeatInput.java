package com.ott.streaming.dto.playback;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaybackHeartbeatInput(
        @NotBlank(message = "Playback token is required")
        String playbackToken,
        @NotNull(message = "Progress seconds are required")
        @Min(value = 0, message = "Progress seconds must be at least 0")
        Integer progressSeconds,
        @NotNull(message = "Duration seconds are required")
        @Min(value = 1, message = "Duration seconds must be greater than 0")
        Integer durationSeconds
) {
}
