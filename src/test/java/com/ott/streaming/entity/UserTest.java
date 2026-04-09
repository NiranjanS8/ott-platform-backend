package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void onCreateSetsAuditTimestamps() {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() throws InterruptedException {
        User user = new User();
        user.setName("Admin");
        user.setEmail("admin@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.onCreate();

        Instant createdAt = user.getCreatedAt();
        Instant initialUpdatedAt = user.getUpdatedAt();

        Thread.sleep(5);
        user.onUpdate();

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }
}
