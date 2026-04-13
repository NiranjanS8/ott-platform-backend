package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.playback.PlaybackSessionPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.PlaybackSessionStatus;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.PlaybackService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(PlaybackGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class PlaybackGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private PlaybackService playbackService;

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void startPlaybackReturnsSessionPayloadForMovie() {
        when(playbackService.startPlayback(org.mockito.ArgumentMatchers.eq("member@example.com"), any()))
                .thenReturn(new PlaybackSessionPayload(
                        1L,
                        9L,
                        ContentType.MOVIE,
                        10L,
                        null,
                        null,
                        "token-123",
                        "https://stream.ott.local/playback/token-123",
                        Instant.parse("2026-04-13T10:00:00Z"),
                        Instant.parse("2026-04-13T10:15:00Z"),
                        Instant.parse("2026-04-13T10:00:00Z"),
                        PlaybackSessionStatus.ACTIVE,
                        Instant.parse("2026-04-13T10:00:00Z"),
                        Instant.parse("2026-04-13T10:00:00Z")
                ));

        graphQlTester.document("""
                mutation {
                  startPlayback(input: {
                    contentType: MOVIE
                    contentId: "10"
                  }) {
                    id
                    contentType
                    contentId
                    playbackToken
                    streamUrl
                    status
                  }
                }
                """)
                .execute()
                .path("startPlayback.id").entity(String.class).isEqualTo("1")
                .path("startPlayback.contentType").entity(String.class).isEqualTo("MOVIE")
                .path("startPlayback.contentId").entity(String.class).isEqualTo("10")
                .path("startPlayback.playbackToken").entity(String.class).isEqualTo("token-123")
                .path("startPlayback.streamUrl").entity(String.class)
                .isEqualTo("https://stream.ott.local/playback/token-123")
                .path("startPlayback.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    void startPlaybackValidationRejectsMissingContentId() {
        graphQlTester.document("""
                mutation {
                  startPlayback(input: {
                    contentType: MOVIE
                  }) {
                    id
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage()).contains("missing required fields '[contentId]'");
                });
    }
}
