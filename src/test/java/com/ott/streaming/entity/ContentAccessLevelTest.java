package com.ott.streaming.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContentAccessLevelTest {

    @Test
    void movieDefaultsToFreeAccessOnCreate() {
        Movie movie = new Movie();
        movie.setTitle("Example Movie");

        movie.onCreate();

        assertThat(movie.getAccessLevel()).isEqualTo(ContentAccessLevel.FREE);
    }

    @Test
    void seriesDefaultsToFreeAccessOnCreate() {
        Series series = new Series();
        series.setTitle("Example Series");

        series.onCreate();

        assertThat(series.getAccessLevel()).isEqualTo(ContentAccessLevel.FREE);
    }
}
