package com.ott.streaming.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.review.ReviewPayload;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.ReviewService;
import com.ott.streaming.entity.ContentType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@GraphQlTest(ReviewGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class ReviewGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void addReviewReturnsPayloadForAuthenticatedUser() {
        when(reviewService.addReview(eq("member@example.com"), any())).thenReturn(
                new ReviewPayload(
                        1L,
                        9L,
                        ContentType.MOVIE,
                        10L,
                        5,
                        "Loved it",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  addReview(input: {
                    contentType: MOVIE
                    contentId: "10"
                    rating: 5
                    comment: "Loved it"
                  }) {
                    id
                    userId
                    contentType
                    rating
                    comment
                  }
                }
                """)
                .execute()
                .path("addReview.id").entity(String.class).isEqualTo("1")
                .path("addReview.userId").entity(String.class).isEqualTo("9")
                .path("addReview.contentType").entity(String.class).isEqualTo("MOVIE")
                .path("addReview.rating").entity(Integer.class).isEqualTo(5)
                .path("addReview.comment").entity(String.class).isEqualTo("Loved it");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void addReviewSurfacesDuplicateErrorWithStableGraphQlShape() {
        when(reviewService.addReview(eq("member@example.com"), any()))
                .thenThrow(ApiException.duplicateResource("You have already reviewed this content"));

        graphQlTester.document("""
                mutation {
                  addReview(input: {
                    contentType: MOVIE
                    contentId: "10"
                    rating: 5
                    comment: "Loved it"
                  }) {
                    id
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage()).isEqualTo("You have already reviewed this content");
                    assertThat(errors.getFirst().getExtensions()).containsEntry("code", "BAD_REQUEST");
                });
    }

    @Test
    void reviewsQueryReturnsReviewList() {
        when(reviewService.getReviews(ContentType.MOVIE, 10L)).thenReturn(List.of(
                new ReviewPayload(
                        1L,
                        9L,
                        ContentType.MOVIE,
                        10L,
                        5,
                        "Loved it",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z")
                )
        ));

        graphQlTester.document("""
                query {
                  reviews(contentType: MOVIE, contentId: "10") {
                    id
                    userId
                    rating
                    comment
                  }
                }
                """)
                .execute()
                .path("reviews[0].id").entity(String.class).isEqualTo("1")
                .path("reviews[0].userId").entity(String.class).isEqualTo("9")
                .path("reviews[0].rating").entity(Integer.class).isEqualTo(5)
                .path("reviews[0].comment").entity(String.class).isEqualTo("Loved it");
    }
}
