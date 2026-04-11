package com.ott.streaming.dto.subscription;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionPlanPayload(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
