package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.review.AddReviewInput;
import com.ott.streaming.dto.review.ReviewPayload;
import com.ott.streaming.dto.review.UpdateReviewInput;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Review;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.ReviewRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeriesRepository seriesRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, userRepository, movieRepository, seriesRepository);
    }

    @Test
    void addReviewCreatesReviewForAuthenticatedUser() {
        User user = buildUser(1L, Role.USER);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(42L)).thenReturn(true);
        when(reviewRepository.findByUserIdAndContentTypeAndContentId(1L, ContentType.MOVIE, 42L))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review savedReview = invocation.getArgument(0);
            savedReview.setId(7L);
            ReflectionTestUtils.setField(savedReview, "createdAt", Instant.parse("2026-04-10T12:00:00Z"));
            ReflectionTestUtils.setField(savedReview, "updatedAt", Instant.parse("2026-04-10T12:00:00Z"));
            return savedReview;
        });

        ReviewPayload payload = reviewService.addReview(
                " Member@Example.com ",
                new AddReviewInput(ContentType.MOVIE, 42L, 5, "  Great movie  ")
        );

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();

        assertThat(savedReview.getUserId()).isEqualTo(1L);
        assertThat(savedReview.getContentType()).isEqualTo(ContentType.MOVIE);
        assertThat(savedReview.getContentId()).isEqualTo(42L);
        assertThat(savedReview.getRating()).isEqualTo(5);
        assertThat(savedReview.getComment()).isEqualTo("Great movie");

        assertThat(payload.id()).isEqualTo(7L);
        assertThat(payload.userId()).isEqualTo(1L);
        assertThat(payload.contentType()).isEqualTo(ContentType.MOVIE);
    }

    @Test
    void addReviewRejectsDuplicateReviewForSameUserAndContent() {
        User user = buildUser(1L, Role.USER);
        Review existingReview = buildReview(9L, 1L, ContentType.MOVIE, 42L, 4);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(42L)).thenReturn(true);
        when(reviewRepository.findByUserIdAndContentTypeAndContentId(1L, ContentType.MOVIE, 42L))
                .thenReturn(Optional.of(existingReview));

        assertThatThrownBy(() -> reviewService.addReview(
                "member@example.com",
                new AddReviewInput(ContentType.MOVIE, 42L, 5, "Another take")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("You have already reviewed this content");
    }

    @Test
    void updateReviewAllowsOwner() {
        User user = buildUser(2L, Role.USER);
        Review review = buildReview(5L, 2L, ContentType.SERIES, 88L, 3);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewPayload payload = reviewService.updateReview(
                "owner@example.com",
                5L,
                new UpdateReviewInput(4, "  Better on rewatch ")
        );

        assertThat(payload.rating()).isEqualTo(4);
        assertThat(payload.comment()).isEqualTo("Better on rewatch");
    }

    @Test
    void updateReviewRejectsDifferentNonAdminUser() {
        User user = buildUser(3L, Role.USER);
        Review review = buildReview(6L, 99L, ContentType.MOVIE, 10L, 2);

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(6L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(
                "other@example.com",
                6L,
                new UpdateReviewInput(4, "Updated")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("You are not allowed to modify this review");
    }

    @Test
    void deleteReviewAllowsAdmin() {
        User admin = buildUser(10L, Role.ADMIN);
        Review review = buildReview(11L, 3L, ContentType.SERIES, 77L, 5);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(review));

        Boolean deleted = reviewService.deleteReview("admin@example.com", 11L);

        assertThat(deleted).isTrue();
        verify(reviewRepository).delete(review);
    }

    @Test
    void addReviewRejectsMissingContent() {
        User user = buildUser(1L, Role.USER);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(seriesRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.addReview(
                "member@example.com",
                new AddReviewInput(ContentType.SERIES, 999L, 4, "Missing series")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Series not found");
    }

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(role == Role.ADMIN ? "Admin" : "Member");
        user.setEmail(role == Role.ADMIN ? "admin@example.com" : "member@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return user;
    }

    private Review buildReview(Long id, Long userId, ContentType contentType, Long contentId, Integer rating) {
        Review review = new Review();
        review.setId(id);
        review.setUserId(userId);
        review.setContentType(contentType);
        review.setContentId(contentId);
        review.setRating(rating);
        review.setComment("Existing");
        ReflectionTestUtils.setField(review, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(review, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return review;
    }
}
