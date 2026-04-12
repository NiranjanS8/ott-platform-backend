package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEpisodeInput(
        @NotNull(message = "Season id is required")
        @Positive(message = "Season id must be greater than 0")
        Long seasonId,
        @NotBlank(message = "Episode title is required")
        @Size(max = 200, message = "Episode title must be at most 200 characters")
        String title,
        @NotNull(message = "Episode number is required")
        @Min(value = 1, message = "Episode number must be at least 1")
        Integer episodeNumber,
        @Size(max = 2000, message = "Episode description must be at most 2000 characters")
        String description,
        @Min(value = 1, message = "Duration minutes must be at least 1")
        Integer durationMinutes,
        String releaseDate
) {
}
