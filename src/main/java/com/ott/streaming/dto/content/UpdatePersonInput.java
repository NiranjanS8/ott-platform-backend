package com.ott.streaming.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePersonInput(
        @NotBlank(message = "Person name is required")
        @Size(max = 150, message = "Person name must be at most 150 characters")
        String name,
        @Size(max = 5000, message = "Biography must be at most 5000 characters")
        String biography,
        @Size(max = 500, message = "Profile image URL must be at most 500 characters")
        String profileImageUrl
) {
}
