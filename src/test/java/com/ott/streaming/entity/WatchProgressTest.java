package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WatchProgressTest {

    @Test
    void onCreateSetsAuditAndLastWatchedTimestamps() {
        WatchProgress progress = new WatchProgress();
        progress.setUserId(1L);
        progress.setContentType(ContentType.MOVIE);
        progress.setContentId(42L);
        progress.setProgressSeconds(120);
        progress.setDurationSeconds(7200);
        progress.setCompleted(false);

        progress.onCreate();

        assertThat(progress.getCreatedAt()).isNotNull();
        assertThat(progress.getUpdatedAt()).isNotNull();
        assertThat(progress.getLastWatchedAt()).isNotNull();
        assertThat(progress.getUpdatedAt()).isEqualTo(progress.getCreatedAt());
        assertThat(progress.getContentType()).isEqualTo(ContentType.MOVIE);
    }

    @Test
    void onCreatePreservesExplicitLastWatchedAt() {
        WatchProgress progress = new WatchProgress();
        Instant explicitTime = Instant.parse("2026-04-11T12:00:00Z");
        progress.setUserId(2L);
        progress.setContentType(ContentType.SERIES);
        progress.setContentId(200L);
        progress.setSeasonId(10L);
        progress.setEpisodeId(20L);
        progress.setProgressSeconds(300);
        progress.setDurationSeconds(3600);
        progress.setCompleted(false);
        progress.setLastWatchedAt(explicitTime);

        progress.onCreate();

        assertThat(progress.getLastWatchedAt()).isEqualTo(explicitTime);
        assertThat(progress.getEpisodeId()).isEqualTo(20L);
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() throws InterruptedException {
        WatchProgress progress = new WatchProgress();
        progress.setUserId(3L);
        progress.setContentType(ContentType.MOVIE);
        progress.setContentId(300L);
        progress.setProgressSeconds(10);
        progress.setDurationSeconds(100);
        progress.setCompleted(false);
        progress.onCreate();

        Instant createdAt = progress.getCreatedAt();
        Instant initialUpdatedAt = progress.getUpdatedAt();

        Thread.sleep(5);
        progress.onUpdate();

        assertThat(progress.getCreatedAt()).isEqualTo(createdAt);
        assertThat(progress.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
