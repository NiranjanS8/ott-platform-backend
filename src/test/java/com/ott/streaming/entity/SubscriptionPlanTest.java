package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SubscriptionPlanTest {

    @Test
    void onCreateSetsAuditTimestamps() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("Premium Monthly");
        plan.setDescription("Premium access for 30 days");
        plan.setPrice(new BigDecimal("9.99"));
        plan.setDurationDays(30);
        plan.setActive(true);

        plan.onCreate();

        assertThat(plan.getCreatedAt()).isNotNull();
        assertThat(plan.getUpdatedAt()).isNotNull();
        assertThat(plan.getUpdatedAt()).isEqualTo(plan.getCreatedAt());
        assertThat(plan.isActive()).isTrue();
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() throws InterruptedException {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("Premium Yearly");
        plan.setPrice(new BigDecimal("99.99"));
        plan.setDurationDays(365);
        plan.setActive(true);
        plan.onCreate();

        Instant createdAt = plan.getCreatedAt();
        Instant initialUpdatedAt = plan.getUpdatedAt();

        Thread.sleep(5);
        plan.onUpdate();

        assertThat(plan.getCreatedAt()).isEqualTo(createdAt);
        assertThat(plan.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
