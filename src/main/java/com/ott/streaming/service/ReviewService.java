package com.ott.streaming.service;

import com.ott.streaming.dto.review.AddReviewInput;
import com.ott.streaming.dto.review.RatingSummaryPayload;
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
import java.util.List;
import java.util.Locale;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         MovieRepository movieRepository,
                         SeriesRepository seriesRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    public ReviewPayload addReview(String email, AddReviewInput input) {
        User currentUser = getAuthenticatedUser(email);
        validateContentExists(input.contentType(), input.contentId());

        reviewRepository.findByUserIdAndContentTypeAndContentId(
                        currentUser.getId(),
                        input.contentType(),
                        input.contentId()
                )
                .ifPresent(existingReview -> {
                    throw ApiException.duplicateResource("You have already reviewed this content");
                });

        Review review = new Review();
        review.setUserId(currentUser.getId());
        review.setContentType(input.contentType());
        review.setContentId(input.contentId());
        review.setRating(input.rating());
        review.setComment(normalizeComment(input.comment()));

        return toPayload(reviewRepository.save(review));
    }

    public ReviewPayload updateReview(String email, Long id, UpdateReviewInput input) {
        User currentUser = getAuthenticatedUser(email);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        validateCanModifyReview(currentUser, review);

        review.setRating(input.rating());
        review.setComment(normalizeComment(input.comment()));

        return toPayload(reviewRepository.save(review));
    }

    public Boolean deleteReview(String email, Long id) {
        User currentUser = getAuthenticatedUser(email);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        validateCanModifyReview(currentUser, review);
        reviewRepository.delete(review);
        return true;
    }

    public List<ReviewPayload> getReviews(ContentType contentType, Long contentId) {
        validateContentExists(contentType, contentId);

        return reviewRepository.findByContentTypeAndContentIdOrderByCreatedAtDesc(contentType, contentId).stream()
                .map(this::toPayload)
                .toList();
    }

    public RatingSummaryPayload getMovieRatingSummary(Long movieId) {
        validateContentExists(ContentType.MOVIE, movieId);
        return buildRatingSummary(ContentType.MOVIE, movieId);
    }

    public RatingSummaryPayload getSeriesRatingSummary(Long seriesId) {
        validateContentExists(ContentType.SERIES, seriesId);
        return buildRatingSummary(ContentType.SERIES, seriesId);
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

    private void validateCanModifyReview(User currentUser, Review review) {
        boolean isOwner = review.getUserId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("You are not allowed to modify this review");
        }
    }

    private ReviewPayload toPayload(Review review) {
        return new ReviewPayload(
                review.getId(),
                review.getUserId(),
                review.getContentType(),
                review.getContentId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private RatingSummaryPayload buildRatingSummary(ContentType contentType, Long contentId) {
        List<Review> reviews = reviewRepository.findByContentTypeAndContentIdOrderByCreatedAtDesc(contentType, contentId);
        if (reviews.isEmpty()) {
            return new RatingSummaryPayload(null, 0);
        }

        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        return new RatingSummaryPayload(average, reviews.size());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }

        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
