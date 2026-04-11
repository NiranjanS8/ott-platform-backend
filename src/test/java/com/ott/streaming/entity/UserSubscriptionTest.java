package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserSubscriptionTest {

    @Test
    void onCreateSetsAuditTimestamps() {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(1L);
        subscription.setPlanId(2L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.parse("2026-04-11T10:00:00Z"));
        subscription.setEndDate(Instant.parse("2026-05-11T10:00:00Z"));

        subscription.onCreate();

        assertThat(subscription.getCreatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isEqualTo(subscription.getCreatedAt());
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() throws InterruptedException {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(1L);
        subscription.setPlanId(2L);
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setStartDate(Instant.parse("2026-04-11T10:00:00Z"));
        subscription.setEndDate(Instant.parse("2026-05-11T10:00:00Z"));
        subscription.onCreate();

        Instant createdAt = subscription.getCreatedAt();
        Instant initialUpdatedAt = subscription.getUpdatedAt();

        Thread.sleep(5);
        subscription.onUpdate();

        assertThat(subscription.getCreatedAt()).isEqualTo(createdAt);
        assertThat(subscription.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
