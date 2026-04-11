package com.ott.streaming.dto.subscription;

import com.ott.streaming.entity.SubscriptionStatus;
import java.time.Instant;

public record UserSubscriptionPayload(
        Long id,
        Long userId,
        Long planId,
        SubscriptionStatus status,
        Instant startDate,
        Instant endDate,
        Instant createdAt,
        Instant updatedAt
) {
}
