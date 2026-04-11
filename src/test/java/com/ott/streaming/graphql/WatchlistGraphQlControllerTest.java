package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.engagement.WatchlistItemPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.WatchlistService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(WatchlistGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class WatchlistGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private WatchlistService watchlistService;

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void addToWatchlistReturnsPayloadForAuthenticatedUser() {
        when(watchlistService.addToWatchlist(eq("member@example.com"), any())).thenReturn(
                new WatchlistItemPayload(
                        1L,
                        9L,
                        ContentType.MOVIE,
                        10L,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  addToWatchlist(input: {
                    contentType: MOVIE
                    contentId: "10"
                  }) {
                    id
                    userId
                    contentType
                    contentId
                  }
                }
                """)
                .execute()
                .path("addToWatchlist.id").entity(String.class).isEqualTo("1")
                .path("addToWatchlist.userId").entity(String.class).isEqualTo("9")
                .path("addToWatchlist.contentType").entity(String.class).isEqualTo("MOVIE")
                .path("addToWatchlist.contentId").entity(String.class).isEqualTo("10");
    }

    @Test
    void addToWatchlistValidationRejectsMissingContentId() {
        graphQlTester.document("""
                mutation {
                  addToWatchlist(input: {
                    contentType: MOVIE
                    contentId: null
                  }) {
                    id
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void myWatchlistReturnsItems() {
        when(watchlistService.getMyWatchlist("member@example.com")).thenReturn(List.of(
                new WatchlistItemPayload(
                        1L,
                        9L,
                        ContentType.SERIES,
                        77L,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        ));

        graphQlTester.document("""
                query {
                  myWatchlist {
                    id
                    contentType
                    contentId
                  }
                }
                """)
                .execute()
                .path("myWatchlist[0].id").entity(String.class).isEqualTo("1")
                .path("myWatchlist[0].contentType").entity(String.class).isEqualTo("SERIES")
                .path("myWatchlist[0].contentId").entity(String.class).isEqualTo("77");
    }
}
