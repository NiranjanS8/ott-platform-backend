package com.ott.streaming.dto.discovery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PaginationInput(
        @Min(value = 0, message = "Page must be zero or greater")
        int page,
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        int size
) {
}
