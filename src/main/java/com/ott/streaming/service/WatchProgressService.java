package com.ott.streaming.service;

import com.ott.streaming.dto.engagement.UpdateWatchProgressInput;
import com.ott.streaming.dto.engagement.WatchProgressPayload;
import com.ott.streaming.entity.ContentType;
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
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WatchProgressService {

    private final WatchProgressRepository watchProgressRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    public WatchProgressService(WatchProgressRepository watchProgressRepository,
                                UserRepository userRepository,
                                MovieRepository movieRepository,
                                SeriesRepository seriesRepository,
                                SeasonRepository seasonRepository,
                                EpisodeRepository episodeRepository) {
        this.watchProgressRepository = watchProgressRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
    }

    public WatchProgressPayload updateWatchProgress(String email, UpdateWatchProgressInput input) {
        User currentUser = getAuthenticatedUser(email);
        validateProgressInput(input);

        WatchProgress progress = findExistingProgress(currentUser.getId(), input)
                .orElseGet(WatchProgress::new);

        progress.setUserId(currentUser.getId());
        progress.setContentType(input.contentType());
        progress.setContentId(input.contentId());
        progress.setSeasonId(input.seasonId());
        progress.setEpisodeId(input.episodeId());
        progress.setProgressSeconds(input.progressSeconds());
        progress.setDurationSeconds(input.durationSeconds());
        progress.setCompleted(input.progressSeconds().equals(input.durationSeconds()));
        progress.setLastWatchedAt(Instant.now());

        return toPayload(watchProgressRepository.save(progress));
    }

    public WatchProgressPayload markAsCompleted(String email, ContentType contentType, Long contentId, Long episodeId) {
        User currentUser = getAuthenticatedUser(email);
        WatchProgress progress = findExistingProgress(currentUser.getId(), contentType, contentId, episodeId)
                .orElseThrow(() -> ApiException.notFound("Watch progress not found"));
                

        progress.setProgressSeconds(progress.getDurationSeconds());
        progress.setCompleted(true);
        progress.setLastWatchedAt(Instant.now());

        return toPayload(watchProgressRepository.save(progress));
    }

    public List<WatchProgressPayload> getContinueWatching(String email) {
        User currentUser = getAuthenticatedUser(email);

        return watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(currentUser.getId()).stream()
                .filter(progress -> !progress.isCompleted())
                .filter(progress -> progress.getProgressSeconds() != null && progress.getProgressSeconds() > 0)
                .map(this::toPayload)
                .toList();
    }

    public List<WatchProgressPayload> getWatchHistory(String email) {
        User currentUser = getAuthenticatedUser(email);

        return watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(currentUser.getId()).stream()
                .map(this::toPayload)
                .toList();
    }

    public WatchProgressPayload syncPlaybackProgress(Long userId,
                                                     ContentType contentType,
                                                     Long contentId,
                                                     Long seasonId,
                                                     Long episodeId,
                                                     Integer progressSeconds,
                                                     Integer durationSeconds,
                                                     boolean completed) {
        validatePlaybackProgress(contentType, progressSeconds, durationSeconds);

        WatchProgress progress = findExistingProgress(userId, contentType, contentId, episodeId)
                .orElseGet(WatchProgress::new);

        progress.setUserId(userId);
        progress.setContentType(contentType);
        progress.setContentId(contentId);
        progress.setSeasonId(seasonId);
        progress.setEpisodeId(episodeId);
        progress.setProgressSeconds(progressSeconds);
        progress.setDurationSeconds(durationSeconds);
        progress.setCompleted(completed);
        progress.setLastWatchedAt(Instant.now());

        return toPayload(watchProgressRepository.save(progress));
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.unauthorized("Authentication is required");
        }

        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> ApiException.unauthorized("Authenticated user not found"));
    }

    private void validateProgressInput(UpdateWatchProgressInput input) {
        validatePlaybackProgress(input.contentType(), input.progressSeconds(), input.durationSeconds());

        switch (input.contentType()) {
            case MOVIE -> validateMovieProgress(input);
            case SERIES -> validateSeriesProgress(input);
        }
    }

    private void validatePlaybackProgress(ContentType contentType, Integer progressSeconds, Integer durationSeconds) {
        if (progressSeconds > durationSeconds) {
            throw ApiException.validation("Progress seconds cannot exceed duration seconds");
        }

        if (contentType == null) {
            throw ApiException.validation("Content type is required");
        }
    }

    private void validateMovieProgress(UpdateWatchProgressInput input) {
        if (!movieRepository.existsById(input.contentId())) {
            throw ApiException.notFound("Movie not found");
        }

        if (input.seasonId() != null || input.episodeId() != null) {
            throw ApiException.validation("Movie progress cannot include season or episode context");
        }
    }

    private void validateSeriesProgress(UpdateWatchProgressInput input) {
        if (!seriesRepository.existsById(input.contentId())) {
            throw ApiException.notFound("Series not found");
        }

        if (input.episodeId() == null) {
            throw ApiException.validation("Series progress requires an episode id");
        }

        if (!episodeRepository.existsById(input.episodeId())) {
            throw ApiException.notFound("Episode not found");
        }

        if (input.seasonId() != null && !seasonRepository.existsById(input.seasonId())) {
            throw ApiException.notFound("Season not found");
        }
    }

    private Optional<WatchProgress> findExistingProgress(Long userId, UpdateWatchProgressInput input) {
        return findExistingProgress(userId, input.contentType(), input.contentId(), input.episodeId());
    }

    private Optional<WatchProgress> findExistingProgress(Long userId,
                                                         ContentType contentType,
                                                         Long contentId,
                                                         Long episodeId) {
        if (contentType == ContentType.MOVIE) {
            return watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeIdIsNull(
                    userId,
                    contentType,
                    contentId
            );
        }

        return watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeId(
                userId,
                contentType,
                contentId,
                episodeId
        );
    }

    private WatchProgressPayload toPayload(WatchProgress progress) {
        return new WatchProgressPayload(
                progress.getId(),
                progress.getUserId(),
                progress.getContentType(),
                progress.getContentId(),
                progress.getSeasonId(),
                progress.getEpisodeId(),
                progress.getProgressSeconds(),
                progress.getDurationSeconds(),
                progress.isCompleted(),
                progress.getLastWatchedAt(),
                progress.getCreatedAt(),
                progress.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
