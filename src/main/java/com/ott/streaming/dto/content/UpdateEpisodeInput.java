package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEpisodeInput(
        @NotBlank(message = "Episode title is required")
        @Size(max = 200, message = "Episode title must be at most 200 characters")
        String title,
        @NotNull(message = "Episode number is required")
        Integer episodeNumber,
        String description,
        Integer durationMinutes,
        String releaseDate
) {
}
