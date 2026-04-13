package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.playback.PlaybackSessionPayload;
import com.ott.streaming.dto.playback.StartPlaybackInput;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.PlaybackSession;
import com.ott.streaming.entity.PlaybackSessionStatus;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.entity.User;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.PlaybackSessionRepository;
import com.ott.streaming.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaybackServiceTest {

    @Mock
    private PlaybackSessionRepository playbackSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private UserSubscriptionService userSubscriptionService;

    private PlaybackService playbackService;

    @BeforeEach
    void setUp() {
        playbackService = new PlaybackService(
                playbackSessionRepository,
                userRepository,
                movieRepository,
                episodeRepository,
                userSubscriptionService,
                Duration.ofMinutes(15),
                "https://stream.ott.local/playback"
        );
    }

    @Test
    void startPlaybackCreatesMovieSessionForAuthenticatedUser() {
        User user = buildUser(9L);
        Movie movie = movie(10L, ContentAccessLevel.FREE);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(playbackSessionRepository.save(any(PlaybackSession.class))).thenAnswer(invocation -> {
            PlaybackSession session = invocation.getArgument(0);
            session.setId(1L);
            ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
            ReflectionTestUtils.setField(session, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));
            return session;
        });

        PlaybackSessionPayload payload = playbackService.startPlayback(
                "member@example.com",
                new StartPlaybackInput(ContentType.MOVIE, 10L, null, null)
        );

        assertThat(payload.id()).isEqualTo(1L);
        assertThat(payload.contentType()).isEqualTo(ContentType.MOVIE);
        assertThat(payload.seasonId()).isNull();
        assertThat(payload.episodeId()).isNull();
        assertThat(payload.playbackToken()).isNotBlank();
        assertThat(payload.streamUrl()).contains(payload.playbackToken());
        assertThat(payload.status()).isEqualTo(PlaybackSessionStatus.ACTIVE);
    }

    @Test
    void startPlaybackBlocksPremiumMovieWithoutSubscription() {
        User user = buildUser(9L);
        Movie movie = movie(10L, ContentAccessLevel.PREMIUM);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(userSubscriptionService.hasPremiumAccess("member@example.com")).thenReturn(false);

        assertThatThrownBy(() -> playbackService.startPlayback(
                "member@example.com",
                new StartPlaybackInput(ContentType.MOVIE, 10L, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Premium subscription required to start playback");
    }

    @Test
    void startPlaybackCreatesSeriesSessionFromEpisodeContext() {
        User user = buildUser(9L);
        Episode episode = episode(33L, 22L, 11L, ContentAccessLevel.FREE);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(episodeRepository.findWithSeasonAndSeriesById(33L)).thenReturn(Optional.of(episode));
        when(playbackSessionRepository.save(any(PlaybackSession.class))).thenAnswer(invocation -> {
            PlaybackSession session = invocation.getArgument(0);
            session.setId(2L);
            ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
            ReflectionTestUtils.setField(session, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));
            return session;
        });

        PlaybackSessionPayload payload = playbackService.startPlayback(
                "member@example.com",
                new StartPlaybackInput(ContentType.SERIES, 11L, 22L, 33L)
        );

        assertThat(payload.id()).isEqualTo(2L);
        assertThat(payload.contentType()).isEqualTo(ContentType.SERIES);
        assertThat(payload.contentId()).isEqualTo(11L);
        assertThat(payload.seasonId()).isEqualTo(22L);
        assertThat(payload.episodeId()).isEqualTo(33L);
    }

    @Test
    void startPlaybackRejectsEpisodeFromDifferentSeries() {
        User user = buildUser(9L);
        Episode episode = episode(33L, 22L, 99L, ContentAccessLevel.FREE);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(episodeRepository.findWithSeasonAndSeriesById(33L)).thenReturn(Optional.of(episode));

        assertThatThrownBy(() -> playbackService.startPlayback(
                "member@example.com",
                new StartPlaybackInput(ContentType.SERIES, 11L, 22L, 33L)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Episode does not belong to the requested series");
    }

    @Test
    void startPlaybackRequiresAuthentication() {
        assertThatThrownBy(() -> playbackService.startPlayback(
                null,
                new StartPlaybackInput(ContentType.MOVIE, 10L, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Authentication is required");
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Member");
        user.setEmail("member@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));
        return user;
    }

    private Movie movie(Long id, ContentAccessLevel accessLevel) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle("The Matrix");
        movie.setAccessLevel(accessLevel);
        movie.setReleaseDate(LocalDate.parse("2026-04-01"));
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));
        return movie;
    }

    private Episode episode(Long episodeId, Long seasonId, Long seriesId, ContentAccessLevel accessLevel) {
        Series series = new Series();
        series.setId(seriesId);
        series.setTitle("Dark");
        series.setAccessLevel(accessLevel);
        series.setReleaseDate(LocalDate.parse("2026-04-01"));
        ReflectionTestUtils.setField(series, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
        ReflectionTestUtils.setField(series, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));

        Season season = new Season();
        season.setId(seasonId);
        season.setTitle("Season 1");
        season.setSeasonNumber(1);
        season.setSeries(series);
        ReflectionTestUtils.setField(season, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
        ReflectionTestUtils.setField(season, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));

        Episode episode = new Episode();
        episode.setId(episodeId);
        episode.setTitle("Episode 1");
        episode.setEpisodeNumber(1);
        episode.setSeason(season);
        ReflectionTestUtils.setField(episode, "createdAt", Instant.parse("2026-04-13T10:00:00Z"));
        ReflectionTestUtils.setField(episode, "updatedAt", Instant.parse("2026-04-13T10:00:00Z"));
        return episode;
    }
}
