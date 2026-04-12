package com.ott.streaming.service;

import com.ott.streaming.dto.engagement.AddToWatchlistInput;
import com.ott.streaming.dto.engagement.WatchlistItemPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.User;
import com.ott.streaming.entity.WatchlistItem;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.repository.WatchlistItemRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    public WatchlistService(WatchlistItemRepository watchlistItemRepository,
                            UserRepository userRepository,
                            MovieRepository movieRepository,
                            SeriesRepository seriesRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    public WatchlistItemPayload addToWatchlist(String email, AddToWatchlistInput input) {
        User currentUser = getAuthenticatedUser(email);
        validateContentExists(input.contentType(), input.contentId());

        if (watchlistItemRepository.existsByUserIdAndContentTypeAndContentId(
                currentUser.getId(),
                input.contentType(),
                input.contentId()
        )) {
            throw ApiException.duplicateResource("Content is already in your watchlist");
        }

        WatchlistItem item = new WatchlistItem();
        item.setUserId(currentUser.getId());
        item.setContentType(input.contentType());
        item.setContentId(input.contentId());

        return toPayload(watchlistItemRepository.save(item));
    }

    public Boolean removeFromWatchlist(String email, ContentType contentType, Long contentId) {
        User currentUser = getAuthenticatedUser(email);

        WatchlistItem item = watchlistItemRepository.findByUserIdAndContentTypeAndContentId(
                        currentUser.getId(),
                        contentType,
                        contentId
                )
                .orElseThrow(() -> new ApiException("Watchlist item not found", ErrorType.NOT_FOUND));
                

        watchlistItemRepository.delete(item);
        return true;
    }

    public List<WatchlistItemPayload> getMyWatchlist(String email) {
        User currentUser = getAuthenticatedUser(email);

        return watchlistItemRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::toPayload)
                .toList();
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.unauthorized("Authentication is required");
        }

        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> ApiException.unauthorized("Authenticated user not found"));
    }

    private void validateContentExists(ContentType contentType, Long contentId) {
        boolean exists = switch (contentType) {
            case MOVIE -> movieRepository.existsById(contentId);
            case SERIES -> seriesRepository.existsById(contentId);
        };

        if (!exists) {
            String label = contentType == ContentType.MOVIE ? "Movie" : "Series";
            throw ApiException.notFound(label + " not found");
        }
    }

    private WatchlistItemPayload toPayload(WatchlistItem item) {
        return new WatchlistItemPayload(
                item.getId(),
                item.getUserId(),
                item.getContentType(),
                item.getContentId(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
