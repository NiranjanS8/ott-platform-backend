package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSeasonInput(
        @NotNull(message = "Series id is required")
        @Positive(message = "Series id must be greater than 0")
        Long seriesId,
        @NotBlank(message = "Season title is required")
        @Size(max = 200, message = "Season title must be at most 200 characters")
        String title,
        @NotNull(message = "Season number is required")
        @Min(value = 1, message = "Season number must be at least 1")
        Integer seasonNumber
) {
}
