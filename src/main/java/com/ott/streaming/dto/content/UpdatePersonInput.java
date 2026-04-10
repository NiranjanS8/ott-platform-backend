package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePersonInput(
        @NotBlank(message = "Person name is required")
        @Size(max = 150, message = "Person name must be at most 150 characters")
        String name,
        String biography,
        String profileImageUrl
) {
}
