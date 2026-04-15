package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.config.CacheConfig;
import com.ott.streaming.dto.subscription.UpdateContentAccessInput;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.ReviewRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.SubscriptionPlanRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        CacheConfig.class,
        ContentReadCacheService.class,
        ContentQueryService.class,
        SubscriptionAdminService.class
})
class ContentCachingIntegrationTest {

    @Autowired
    private ContentQueryService contentQueryService;

    @Autowired
    private SubscriptionAdminService subscriptionAdminService;

    @MockitoBean
    private MovieRepository movieRepository;

    @MockitoBean
    private SeriesRepository seriesRepository;

    @MockitoBean
    private SeasonRepository seasonRepository;

    @MockitoBean
    private EpisodeRepository episodeRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Test
    void getMoviesUsesCacheAcrossRepeatedReads() {
        when(movieRepository.findAll()).thenReturn(List.of(movie(1L, "The Matrix", ContentAccessLevel.FREE)));

        var first = contentQueryService.getMovies();
        var second = contentQueryService.getMovies();

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    void updateMovieAccessLevelEvictsCachedMoviePayload() {
        Movie cachedMovie = movie(1L, "The Matrix", ContentAccessLevel.FREE);
        Movie movieForAccessUpdate = movie(1L, "The Matrix", ContentAccessLevel.FREE);
        Movie refreshedMovie = movie(1L, "The Matrix", ContentAccessLevel.PREMIUM);

        when(movieRepository.findById(1L)).thenReturn(
                Optional.of(cachedMovie),
                Optional.of(movieForAccessUpdate),
                Optional.of(refreshedMovie)
        );
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var firstRead = contentQueryService.getMovieById(1L);
        var updated = subscriptionAdminService.updateMovieAccessLevel(
                1L,
                new UpdateContentAccessInput(ContentAccessLevel.PREMIUM)
        );
        var secondRead = contentQueryService.getMovieById(1L);

        assertThat(firstRead).isNotNull();
        assertThat(firstRead.accessLevel()).isEqualTo(ContentAccessLevel.FREE);
        assertThat(updated.accessLevel()).isEqualTo(ContentAccessLevel.PREMIUM);
        assertThat(secondRead).isNotNull();
        assertThat(secondRead.accessLevel()).isEqualTo(ContentAccessLevel.PREMIUM);
        verify(movieRepository, times(3)).findById(1L);
    }

    private Movie movie(Long id, String title, ContentAccessLevel accessLevel) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setAccessLevel(accessLevel);
        movie.setReleaseDate(LocalDate.parse("2026-04-01"));
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return movie;
    }
}
