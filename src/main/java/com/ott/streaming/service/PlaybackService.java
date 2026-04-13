package com.ott.streaming.service;

import com.ott.streaming.dto.playback.PlaybackSessionPayload;
import com.ott.streaming.dto.playback.StartPlaybackInput;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.PlaybackSession;
import com.ott.streaming.entity.PlaybackSessionStatus;
import com.ott.streaming.entity.Series;
import com.ott.streaming.entity.User;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.PlaybackSessionRepository;
import com.ott.streaming.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlaybackService {

    private final PlaybackSessionRepository playbackSessionRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final UserSubscriptionService userSubscriptionService;
    private final Duration playbackSessionTtl;
    private final String playbackBaseUrl;

    public PlaybackService(PlaybackSessionRepository playbackSessionRepository,
                           UserRepository userRepository,
                           MovieRepository movieRepository,
                           EpisodeRepository episodeRepository,
                           UserSubscriptionService userSubscriptionService,
                           @Value("${app.playback.session-ttl:PT15M}") Duration playbackSessionTtl,
                           @Value("${app.playback.base-url:https://stream.ott.local/playback}") String playbackBaseUrl) {
        this.playbackSessionRepository = playbackSessionRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.episodeRepository = episodeRepository;
        this.userSubscriptionService = userSubscriptionService;
        this.playbackSessionTtl = playbackSessionTtl;
        this.playbackBaseUrl = playbackBaseUrl;
    }

    public PlaybackSessionPayload startPlayback(String email, StartPlaybackInput input) {
        User currentUser = getAuthenticatedUser(email);
        PlaybackTarget playbackTarget = resolvePlaybackTarget(email, input);
        Instant now = Instant.now();

        PlaybackSession session = new PlaybackSession();
        session.setUserId(currentUser.getId());
        session.setContentType(input.contentType());
        session.setContentId(input.contentId());
        session.setSeasonId(playbackTarget.seasonId());
        session.setEpisodeId(playbackTarget.episodeId());
        session.setPlaybackToken(UUID.randomUUID().toString());
        session.setStartedAt(now);
        session.setLastHeartbeatAt(now);
        session.setExpiresAt(now.plus(playbackSessionTtl));
        session.setStatus(PlaybackSessionStatus.ACTIVE);

        return toPayload(playbackSessionRepository.save(session));
    }

    private PlaybackTarget resolvePlaybackTarget(String email, StartPlaybackInput input) {
        return switch (input.contentType()) {
            case MOVIE -> resolveMovieTarget(email, input);
            case SERIES -> resolveSeriesTarget(email, input);
        };
    }

    private PlaybackTarget resolveMovieTarget(String email, StartPlaybackInput input) {
        if (input.seasonId() != null || input.episodeId() != null) {
            throw ApiException.validation("Movie playback does not support season or episode ids");
        }

        Movie movie = movieRepository.findById(input.contentId())
                .orElseThrow(() -> ApiException.notFound("Movie not found"));
        enforceAccess(email, movie.getAccessLevel());
        return new PlaybackTarget(null, null);
    }

    private PlaybackTarget resolveSeriesTarget(String email, StartPlaybackInput input) {
        if (input.episodeId() == null) {
            throw ApiException.validation("Series playback requires an episode id");
        }

        Episode episode = episodeRepository.findWithSeasonAndSeriesById(input.episodeId())
                .orElseThrow(() -> ApiException.notFound("Episode not found"));
        Series series = episode.getSeason().getSeries();

        if (!series.getId().equals(input.contentId())) {
            throw ApiException.validation("Episode does not belong to the requested series");
        }

        if (input.seasonId() != null && !episode.getSeason().getId().equals(input.seasonId())) {
            throw ApiException.validation("Episode does not belong to the requested season");
        }

        enforceAccess(email, series.getAccessLevel());
        return new PlaybackTarget(episode.getSeason().getId(), episode.getId());
    }

    private void enforceAccess(String email, ContentAccessLevel accessLevel) {
        if (accessLevel != ContentAccessLevel.PREMIUM) {
            return;
        }

        if (!userSubscriptionService.hasPremiumAccess(email)) {
            throw ApiException.forbidden("Premium subscription required to start playback");
        }
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.unauthorized("Authentication is required");
        }

        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> ApiException.unauthorized("Authenticated user not found"));
    }

    private PlaybackSessionPayload toPayload(PlaybackSession session) {
        return new PlaybackSessionPayload(
                session.getId(),
                session.getUserId(),
                session.getContentType(),
                session.getContentId(),
                session.getSeasonId(),
                session.getEpisodeId(),
                session.getPlaybackToken(),
                buildStreamUrl(session.getPlaybackToken()),
                session.getStartedAt(),
                session.getExpiresAt(),
                session.getLastHeartbeatAt(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private String buildStreamUrl(String playbackToken) {
        String normalizedBaseUrl = playbackBaseUrl.endsWith("/") ? playbackBaseUrl.substring(0, playbackBaseUrl.length() - 1) : playbackBaseUrl;
        return normalizedBaseUrl + "/" + playbackToken;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record PlaybackTarget(Long seasonId, Long episodeId) {
    }
}
