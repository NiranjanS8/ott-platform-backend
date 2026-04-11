package com.ott.streaming.dto.subscription;

import com.ott.streaming.entity.ContentAccessLevel;
import jakarta.validation.constraints.NotNull;

public record UpdateContentAccessInput(
        @NotNull(message = "Access level is required")
        ContentAccessLevel accessLevel
) {
}
