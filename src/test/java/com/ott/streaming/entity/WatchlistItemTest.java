package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WatchlistItemTest {

    @Test
    void onCreateSetsAuditTimestamps() {
        WatchlistItem item = new WatchlistItem();
        item.setUserId(1L);
        item.setContentType(ContentType.MOVIE);
        item.setContentId(42L);

        item.onCreate();

        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isEqualTo(item.getCreatedAt());
        assertThat(item.getContentType()).isEqualTo(ContentType.MOVIE);
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() throws InterruptedException {
        WatchlistItem item = new WatchlistItem();
        item.setUserId(2L);
        item.setContentType(ContentType.SERIES);
        item.setContentId(100L);
        item.onCreate();

        Instant createdAt = item.getCreatedAt();
        Instant initialUpdatedAt = item.getUpdatedAt();

        Thread.sleep(5);
        item.onUpdate();

        assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        assertThat(item.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(item.getContentType()).isEqualTo(ContentType.SERIES);
    }
}
