package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.engagement.UpdateWatchProgressInput;
import com.ott.streaming.dto.engagement.WatchProgressPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.entity.User;
import com.ott.streaming.entity.WatchProgress;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.repository.WatchProgressRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WatchProgressServiceTest {

    @Mock
    private WatchProgressRepository watchProgressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    private WatchProgressService watchProgressService;

    @BeforeEach
    void setUp() {
        watchProgressService = new WatchProgressService(
                watchProgressRepository,
                userRepository,
                movieRepository,
                seriesRepository,
                seasonRepository,
                episodeRepository
        );
    }

    @Test
    void updateWatchProgressCreatesMovieProgress() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(42L)).thenReturn(true);
        when(watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeIdIsNull(
                1L, ContentType.MOVIE, 42L
        )).thenReturn(Optional.empty());
        when(watchProgressRepository.save(any(WatchProgress.class))).thenAnswer(invocation -> {
            WatchProgress progress = invocation.getArgument(0);
            progress.setId(7L);
            ReflectionTestUtils.setField(progress, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
            ReflectionTestUtils.setField(progress, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
            return progress;
        });

        WatchProgressPayload payload = watchProgressService.updateWatchProgress(
                " Member@Example.com ",
                new UpdateWatchProgressInput(ContentType.MOVIE, 42L, null, null, 120, 7200)
        );

        ArgumentCaptor<WatchProgress> captor = ArgumentCaptor.forClass(WatchProgress.class);
        verify(watchProgressRepository).save(captor.capture());
        WatchProgress saved = captor.getValue();

        assertThat(saved.getContentType()).isEqualTo(ContentType.MOVIE);
        assertThat(saved.getEpisodeId()).isNull();
        assertThat(saved.getProgressSeconds()).isEqualTo(120);
        assertThat(payload.id()).isEqualTo(7L);
        assertThat(payload.completed()).isFalse();
    }

    @Test
    void updateWatchProgressCreatesSeriesEpisodeProgress() {
        User user = buildUser(1L);
        Episode episode = buildEpisode(9L, 8L, 77L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(seriesRepository.existsById(77L)).thenReturn(true);
        when(episodeRepository.findWithSeasonAndSeriesById(9L)).thenReturn(Optional.of(episode));
        when(watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeId(
                1L, ContentType.SERIES, 77L, 9L
        )).thenReturn(Optional.empty());
        when(watchProgressRepository.save(any(WatchProgress.class))).thenAnswer(invocation -> {
            WatchProgress progress = invocation.getArgument(0);
            progress.setId(8L);
            ReflectionTestUtils.setField(progress, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
            ReflectionTestUtils.setField(progress, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
            return progress;
        });

        WatchProgressPayload payload = watchProgressService.updateWatchProgress(
                "member@example.com",
                new UpdateWatchProgressInput(ContentType.SERIES, 77L, 8L, 9L, 1800, 3600)
        );

        assertThat(payload.contentType()).isEqualTo(ContentType.SERIES);
        assertThat(payload.seasonId()).isEqualTo(8L);
        assertThat(payload.episodeId()).isEqualTo(9L);
        assertThat(payload.completed()).isFalse();
    }

    @Test
    void updateWatchProgressRejectsSeriesEpisodeFromDifferentSeries() {
        User user = buildUser(1L);
        Episode episode = buildEpisode(9L, 8L, 99L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(seriesRepository.existsById(77L)).thenReturn(true);
        when(episodeRepository.findWithSeasonAndSeriesById(9L)).thenReturn(Optional.of(episode));

        assertThatThrownBy(() -> watchProgressService.updateWatchProgress(
                "member@example.com",
                new UpdateWatchProgressInput(ContentType.SERIES, 77L, 8L, 9L, 1800, 3600)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Episode does not belong to the requested series");
    }

    @Test
    void updateWatchProgressRejectsMovieWithEpisodeContext() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(42L)).thenReturn(true);

        assertThatThrownBy(() -> watchProgressService.updateWatchProgress(
                "member@example.com",
                new UpdateWatchProgressInput(ContentType.MOVIE, 42L, 8L, 9L, 100, 200)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Movie progress cannot include season or episode context");
    }

    @Test
    void updateWatchProgressRejectsSeriesWithoutEpisode() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(seriesRepository.existsById(77L)).thenReturn(true);

        assertThatThrownBy(() -> watchProgressService.updateWatchProgress(
                "member@example.com",
                new UpdateWatchProgressInput(ContentType.SERIES, 77L, null, null, 100, 200)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Series progress requires an episode id");
    }

    @Test
    void markAsCompletedSetsProgressToDuration() {
        User user = buildUser(1L);
        WatchProgress existing = buildProgress(5L, 1L, ContentType.MOVIE, 42L, null, null, 120, 7200, false);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeIdIsNull(
                1L, ContentType.MOVIE, 42L
        )).thenReturn(Optional.of(existing));
        when(watchProgressRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        WatchProgressPayload payload = watchProgressService.markAsCompleted(
                "member@example.com",
                ContentType.MOVIE,
                42L,
                null
        );

        assertThat(payload.completed()).isTrue();
        assertThat(payload.progressSeconds()).isEqualTo(7200);
    }

    @Test
    void updateWatchProgressRejectsProgressBeyondDuration() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> watchProgressService.updateWatchProgress(
                "member@example.com",
                new UpdateWatchProgressInput(ContentType.MOVIE, 42L, null, null, 500, 100)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Progress seconds cannot exceed duration seconds");
    }

    @Test
    void getContinueWatchingReturnsOnlyInProgressItemsOrderedByRecentActivity() {
        User user = buildUser(1L);
        WatchProgress recentInProgress = buildProgress(1L, 1L, ContentType.SERIES, 77L, 8L, 9L, 600, 3600, false);
        recentInProgress.setLastWatchedAt(Instant.parse("2026-04-11T12:00:00Z"));
        WatchProgress completed = buildProgress(2L, 1L, ContentType.MOVIE, 42L, null, null, 7200, 7200, true);
        completed.setLastWatchedAt(Instant.parse("2026-04-11T11:00:00Z"));
        WatchProgress zeroProgress = buildProgress(3L, 1L, ContentType.MOVIE, 99L, null, null, 0, 5400, false);
        zeroProgress.setLastWatchedAt(Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(1L))
                .thenReturn(List.of(recentInProgress, completed, zeroProgress));

        List<WatchProgressPayload> result = watchProgressService.getContinueWatching("member@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).contentType()).isEqualTo(ContentType.SERIES);
        assertThat(result.get(0).episodeId()).isEqualTo(9L);
    }

    @Test
    void getWatchHistoryReturnsAllEntriesOrderedByRecentActivity() {
        User user = buildUser(1L);
        WatchProgress recent = buildProgress(1L, 1L, ContentType.MOVIE, 42L, null, null, 120, 7200, false);
        recent.setLastWatchedAt(Instant.parse("2026-04-11T12:00:00Z"));
        WatchProgress older = buildProgress(2L, 1L, ContentType.SERIES, 77L, 8L, 9L, 3600, 3600, true);
        older.setLastWatchedAt(Instant.parse("2026-04-11T11:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(1L))
                .thenReturn(List.of(recent, older));

        List<WatchProgressPayload> result = watchProgressService.getWatchHistory("member@example.com");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentId()).isEqualTo(42L);
        assertThat(result.get(1).contentId()).isEqualTo(77L);
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Member");
        user.setEmail("member@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
        return user;
    }

    private WatchProgress buildProgress(Long id,
                                        Long userId,
                                        ContentType contentType,
                                        Long contentId,
                                        Long seasonId,
                                        Long episodeId,
                                        Integer progressSeconds,
                                        Integer durationSeconds,
                                        boolean completed) {
        WatchProgress progress = new WatchProgress();
        progress.setId(id);
        progress.setUserId(userId);
        progress.setContentType(contentType);
        progress.setContentId(contentId);
        progress.setSeasonId(seasonId);
        progress.setEpisodeId(episodeId);
        progress.setProgressSeconds(progressSeconds);
        progress.setDurationSeconds(durationSeconds);
        progress.setCompleted(completed);
        progress.setLastWatchedAt(Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(progress, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(progress, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
        return progress;
    }

    private Episode buildEpisode(Long episodeId, Long seasonId, Long seriesId) {
        Series series = new Series();
        series.setId(seriesId);
        series.setTitle("Series");

        Season season = new Season();
        season.setId(seasonId);
        season.setTitle("Season 1");
        season.setSeasonNumber(1);
        season.setSeries(series);

        Episode episode = new Episode();
        episode.setId(episodeId);
        episode.setTitle("Episode 1");
        episode.setEpisodeNumber(1);
        episode.setSeason(season);
        return episode;
    }
}
