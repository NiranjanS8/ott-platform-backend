package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateSeriesInput(
        @NotBlank(message = "Series title is required")
        @Size(max = 200, message = "Series title must be at most 200 characters")
        String title,
        String description,
        String releaseDate,
        String endDate,
        String maturityRating,
        @NotNull(message = "Genre ids are required")
        Set<Long> genreIds,
        @NotNull(message = "Cast ids are required")
        Set<Long> castIds,
        @NotNull(message = "Director ids are required")
        Set<Long> directorIds
) {
}
