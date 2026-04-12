package com.ott.streaming.dto.subscription;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateSubscriptionPlanInput(
        @NotBlank(message = "Plan name is required")
        @Size(max = 100, message = "Plan name must be at most 100 characters")
        String name,
        String description,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be at least 0")
        BigDecimal price,
        @NotNull(message = "Duration days are required")
        @Min(value = 1, message = "Duration days must be at least 1")
        Integer durationDays,
        @NotNull(message = "Active flag is required")
        Boolean active
) {
}
