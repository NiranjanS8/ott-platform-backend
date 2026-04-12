package com.ott.streaming.dto.subscription;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscribeToPlanInput(
        @NotNull(message = "Plan id is required")
        @Positive(message = "Plan id must be greater than 0")
        Long planId
) {
}
