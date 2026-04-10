package com.ott.streaming.service;

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
                    throw new ApiException("You have already reviewed this content");
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
                .orElseThrow(() -> new ApiException("Review not found", ErrorType.NOT_FOUND));

        validateCanModifyReview(currentUser, review);

        review.setRating(input.rating());
        review.setComment(normalizeComment(input.comment()));

        return toPayload(reviewRepository.save(review));
    }

    public Boolean deleteReview(String email, Long id) {
        User currentUser = getAuthenticatedUser(email);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ApiException("Review not found", ErrorType.NOT_FOUND));

        validateCanModifyReview(currentUser, review);
        reviewRepository.delete(review);
        return true;
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException("Authentication is required", ErrorType.UNAUTHORIZED);
        }

        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException("Authenticated user not found", ErrorType.UNAUTHORIZED));
    }

    private void validateContentExists(ContentType contentType, Long contentId) {
        boolean exists = switch (contentType) {
            case MOVIE -> movieRepository.existsById(contentId);
            case SERIES -> seriesRepository.existsById(contentId);
        };

        if (!exists) {
            String label = contentType == ContentType.MOVIE ? "Movie" : "Series";
            throw new ApiException(label + " not found", ErrorType.NOT_FOUND);
        }
    }

    private void validateCanModifyReview(User currentUser, Review review) {
        boolean isOwner = review.getUserId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ApiException("You are not allowed to modify this review", ErrorType.FORBIDDEN);
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
