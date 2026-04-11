package com.ott.streaming.dto.subscription;

import jakarta.validation.constraints.NotNull;

public record SubscribeToPlanInput(
        @NotNull(message = "Plan id is required")
        Long planId
) {
}
