package com.ott.streaming.graphql;

import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.review.AddReviewInput;
import com.ott.streaming.dto.review.RatingSummaryPayload;
import com.ott.streaming.dto.review.ReviewPayload;
import com.ott.streaming.dto.review.UpdateReviewInput;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ReviewGraphQlController {

    private final ReviewService reviewService;

    public ReviewGraphQlController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @MutationMapping
    public ReviewPayload addReview(@AuthenticationPrincipal(expression = "username") String email,
                                   @Argument @Valid AddReviewInput input) {
        return reviewService.addReview(email, input);
    }

    @MutationMapping
    public ReviewPayload updateReview(@AuthenticationPrincipal(expression = "username") String email,
                                      @Argument Long id,
                                      @Argument @Valid UpdateReviewInput input) {
        return reviewService.updateReview(email, id, input);
    }

    @MutationMapping
    public Boolean deleteReview(@AuthenticationPrincipal(expression = "username") String email, @Argument Long id) {
        return reviewService.deleteReview(email, id);
    }

    @QueryMapping
    public List<ReviewPayload> reviews(@Argument ContentType contentType, @Argument Long contentId) {
        return reviewService.getReviews(contentType, contentId);
    }

    @SchemaMapping(typeName = "Movie", field = "ratingSummary")
    public CompletableFuture<RatingSummaryPayload> movieRatingSummary(
            MoviePayload source,
            DataLoader<Long, RatingSummaryPayload> movieRatingSummaryDataLoader
    ) {
        return movieRatingSummaryDataLoader.load(source.id());
    }

    @SchemaMapping(typeName = "Series", field = "ratingSummary")
    public CompletableFuture<RatingSummaryPayload> seriesRatingSummary(
            SeriesPayload source,
            DataLoader<Long, RatingSummaryPayload> seriesRatingSummaryDataLoader
    ) {
        return seriesRatingSummaryDataLoader.load(source.id());
    }
}
