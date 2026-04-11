package com.ott.streaming.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.engagement.WatchProgressPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.WatchProgressService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(WatchProgressGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class WatchProgressGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private WatchProgressService watchProgressService;

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void updateWatchProgressReturnsPayloadForMovieProgress() {
        when(watchProgressService.updateWatchProgress(eq("member@example.com"), any())).thenReturn(
                new WatchProgressPayload(
                        1L,
                        9L,
                        ContentType.MOVIE,
                        10L,
                        null,
                        null,
                        120,
                        7200,
                        false,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  updateWatchProgress(input: {
                    contentType: MOVIE
                    contentId: "10"
                    progressSeconds: 120
                    durationSeconds: 7200
                  }) {
                    id
                    contentType
                    progressSeconds
                    durationSeconds
                    completed
                  }
                }
                """)
                .execute()
                .path("updateWatchProgress.id").entity(String.class).isEqualTo("1")
                .path("updateWatchProgress.contentType").entity(String.class).isEqualTo("MOVIE")
                .path("updateWatchProgress.progressSeconds").entity(Integer.class).isEqualTo(120)
                .path("updateWatchProgress.durationSeconds").entity(Integer.class).isEqualTo(7200)
                .path("updateWatchProgress.completed").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void markAsCompletedReturnsCompletedPayload() {
        when(watchProgressService.markAsCompleted("member@example.com", ContentType.SERIES, 77L, 9L)).thenReturn(
                new WatchProgressPayload(
                        2L,
                        9L,
                        ContentType.SERIES,
                        77L,
                        8L,
                        9L,
                        3600,
                        3600,
                        true,
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z"),
                        Instant.parse("2026-04-11T10:00:00Z")
                )
        );

        graphQlTester.document("""
                mutation {
                  markAsCompleted(contentType: SERIES, contentId: "77", episodeId: "9") {
                    id
                    contentType
                    episodeId
                    progressSeconds
                    durationSeconds
                    completed
                  }
                }
                """)
                .execute()
                .path("markAsCompleted.id").entity(String.class).isEqualTo("2")
                .path("markAsCompleted.contentType").entity(String.class).isEqualTo("SERIES")
                .path("markAsCompleted.episodeId").entity(String.class).isEqualTo("9")
                .path("markAsCompleted.completed").entity(Boolean.class).isEqualTo(true);
    }
}
