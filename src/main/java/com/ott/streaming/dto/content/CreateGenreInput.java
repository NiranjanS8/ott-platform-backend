package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGenreInput(
        @NotBlank(message = "Genre name is required")
        @Size(max = 100, message = "Genre name must be at most 100 characters")
        String name
) {
}
