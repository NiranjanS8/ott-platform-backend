package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.ott.streaming.exception.GraphQlExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(ReviewGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class ReviewGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void addReviewValidationRejectsOutOfRangeRating() {
        graphQlTester.document("""
                mutation {
                  addReview(input: {
                    contentType: MOVIE
                    contentId: "10"
                    rating: 0
                    comment: "Too low"
                  }) {
                    id
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage()).contains("Rating must be between 1 and 5");
                });
    }
}
