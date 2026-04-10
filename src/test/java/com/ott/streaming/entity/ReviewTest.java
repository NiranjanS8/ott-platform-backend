package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReviewTest {

    @Test
    void onCreateSetsAuditTimestamps() {
        Review review = new Review();
        review.setUserId(1L);
        review.setContentType(ContentType.MOVIE);
        review.setContentId(42L);
        review.setRating(5);
        review.setComment("Great movie.");

        review.onCreate();

        assertThat(review.getCreatedAt()).isNotNull();
        assertThat(review.getUpdatedAt()).isNotNull();
        assertThat(review.getUpdatedAt()).isEqualTo(review.getCreatedAt());
        assertThat(review.getContentType()).isEqualTo(ContentType.MOVIE);
        assertThat(review.getRating()).isEqualTo(5);
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() throws InterruptedException {
        Review review = new Review();
        review.setUserId(2L);
        review.setContentType(ContentType.SERIES);
        review.setContentId(100L);
        review.setRating(4);
        review.onCreate();

        Instant createdAt = review.getCreatedAt();
        Instant initialUpdatedAt = review.getUpdatedAt();

        Thread.sleep(5);
        review.onUpdate();

        assertThat(review.getCreatedAt()).isEqualTo(createdAt);
        assertThat(review.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(review.getContentType()).isEqualTo(ContentType.SERIES);
    }
}
