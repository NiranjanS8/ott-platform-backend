package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.engagement.AddToWatchlistInput;
import com.ott.streaming.dto.engagement.WatchlistItemPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.entity.WatchlistItem;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.repository.WatchlistItemRepository;
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
class WatchlistServiceTest {

    @Mock
    private WatchlistItemRepository watchlistItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeriesRepository seriesRepository;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(
                watchlistItemRepository,
                userRepository,
                movieRepository,
                seriesRepository
        );
    }

    @Test
    void addToWatchlistCreatesItemForAuthenticatedUser() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(42L)).thenReturn(true);
        when(watchlistItemRepository.existsByUserIdAndContentTypeAndContentId(1L, ContentType.MOVIE, 42L))
                .thenReturn(false);
        when(watchlistItemRepository.save(any(WatchlistItem.class))).thenAnswer(invocation -> {
            WatchlistItem item = invocation.getArgument(0);
            item.setId(7L);
            ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
            ReflectionTestUtils.setField(item, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
            return item;
        });

        WatchlistItemPayload payload = watchlistService.addToWatchlist(
                " Member@Example.com ",
                new AddToWatchlistInput(ContentType.MOVIE, 42L)
        );

        ArgumentCaptor<WatchlistItem> captor = ArgumentCaptor.forClass(WatchlistItem.class);
        verify(watchlistItemRepository).save(captor.capture());
        WatchlistItem savedItem = captor.getValue();

        assertThat(savedItem.getUserId()).isEqualTo(1L);
        assertThat(savedItem.getContentType()).isEqualTo(ContentType.MOVIE);
        assertThat(savedItem.getContentId()).isEqualTo(42L);
        assertThat(payload.id()).isEqualTo(7L);
        assertThat(payload.userId()).isEqualTo(1L);
    }

    @Test
    void addToWatchlistRejectsDuplicateEntries() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(seriesRepository.existsById(77L)).thenReturn(true);
        when(watchlistItemRepository.existsByUserIdAndContentTypeAndContentId(1L, ContentType.SERIES, 77L))
                .thenReturn(true);

        assertThatThrownBy(() -> watchlistService.addToWatchlist(
                "member@example.com",
                new AddToWatchlistInput(ContentType.SERIES, 77L)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Content is already in your watchlist");
    }

    @Test
    void removeFromWatchlistDeletesExistingItem() {
        User user = buildUser(1L);
        WatchlistItem item = buildItem(9L, 1L, ContentType.MOVIE, 42L);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchlistItemRepository.findByUserIdAndContentTypeAndContentId(1L, ContentType.MOVIE, 42L))
                .thenReturn(Optional.of(item));

        Boolean removed = watchlistService.removeFromWatchlist("member@example.com", ContentType.MOVIE, 42L);

        assertThat(removed).isTrue();
        verify(watchlistItemRepository).delete(item);
    }

    @Test
    void getMyWatchlistReturnsNewestFirst() {
        User user = buildUser(1L);
        WatchlistItem newer = buildItem(2L, 1L, ContentType.SERIES, 77L);
        WatchlistItem older = buildItem(1L, 1L, ContentType.MOVIE, 42L);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchlistItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(newer, older));

        List<WatchlistItemPayload> watchlist = watchlistService.getMyWatchlist("member@example.com");

        assertThat(watchlist).hasSize(2);
        assertThat(watchlist.get(0).contentType()).isEqualTo(ContentType.SERIES);
        assertThat(watchlist.get(1).contentType()).isEqualTo(ContentType.MOVIE);
    }

    @Test
    void addToWatchlistRejectsMissingContent() {
        User user = buildUser(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> watchlistService.addToWatchlist(
                "member@example.com",
                new AddToWatchlistInput(ContentType.MOVIE, 999L)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Movie not found");
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

    private WatchlistItem buildItem(Long id, Long userId, ContentType contentType, Long contentId) {
        WatchlistItem item = new WatchlistItem();
        item.setId(id);
        item.setUserId(userId);
        item.setContentType(contentType);
        item.setContentId(contentId);
        ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(item, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
        return item;
    }
}
