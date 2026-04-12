package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.ott.streaming.config.GraphQlDataLoaderConfig;
import com.ott.streaming.config.GraphQlObservabilityConfig;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.ContentAdminService;
import com.ott.streaming.service.ContentQueryService;
import com.ott.streaming.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(ContentGraphQlController.class)
@Import({GraphQlExceptionHandler.class, GraphQlDataLoaderConfig.class, GraphQlObservabilityConfig.class})
@TestPropertySource(properties = {
        "app.graphql.max-query-depth=2",
        "app.graphql.log-requests=false"
})
class GraphQlGuardrailsTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ContentAdminService contentAdminService;

    @MockitoBean
    private ContentQueryService contentQueryService;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void rejectsQueriesThatExceedConfiguredDepth() {
        MoviePayload movie = new MoviePayload(1L, "The Matrix", "Sci-fi action film", "1999-03-31", 136, "R", "English",
                ContentAccessLevel.FREE,
                now(), now());
        when(contentQueryService.getMovieById(1L)).thenReturn(movie);
        when(contentQueryService.getMovieGenresByMovieIds(argThat(ids -> ids.contains(1L))))
                .thenReturn(java.util.Map.of(1L, List.of(new GenrePayload(10L, "Action", now(), now()))));

        graphQlTester.document("""
                query {
                  movie(id: "1") {
                    genres {
                      name
                    }
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.getFirst().getMessage()).contains("maximum query depth exceeded");
                });
    }

    private Instant now() {
        return Instant.parse("2026-04-10T10:00:00Z");
    }
}
