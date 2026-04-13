package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.playback.PlaybackAccessPayload;
import com.ott.streaming.dto.playback.PlaybackSessionPayload;
import com.ott.streaming.entity.ContentAccessLevel;
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

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void heartbeatPlaybackReturnsExtendedActiveSession() {
        when(playbackService.heartbeat(org.mockito.ArgumentMatchers.eq("member@example.com"), any()))
                .thenReturn(new PlaybackSessionPayload(
                        2L,
                        9L,
                        ContentType.MOVIE,
                        10L,
                        null,
                        null,
                        "token-123",
                        "https://stream.ott.local/playback/token-123",
                        Instant.parse("2026-04-13T10:00:00Z"),
                        Instant.parse("2026-04-13T10:20:00Z"),
                        Instant.parse("2026-04-13T10:05:00Z"),
                        PlaybackSessionStatus.ACTIVE,
                        Instant.parse("2026-04-13T10:00:00Z"),
                        Instant.parse("2026-04-13T10:05:00Z")
                ));

        graphQlTester.document("""
                mutation {
                  heartbeatPlayback(input: {
                    playbackToken: "token-123"
                    progressSeconds: 300
                    durationSeconds: 7200
                  }) {
                    id
                    playbackToken
                    status
                  }
                }
                """)
                .execute()
                .path("heartbeatPlayback.id").entity(String.class).isEqualTo("2")
                .path("heartbeatPlayback.playbackToken").entity(String.class).isEqualTo("token-123")
                .path("heartbeatPlayback.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void stopPlaybackReturnsStoppedSession() {
        when(playbackService.stopPlayback(org.mockito.ArgumentMatchers.eq("member@example.com"), any()))
                .thenReturn(new PlaybackSessionPayload(
                        3L,
                        9L,
                        ContentType.SERIES,
                        77L,
                        8L,
                        9L,
                        "token-stop",
                        "https://stream.ott.local/playback/token-stop",
                        Instant.parse("2026-04-13T10:00:00Z"),
                        Instant.parse("2026-04-13T10:10:00Z"),
                        Instant.parse("2026-04-13T10:10:00Z"),
                        PlaybackSessionStatus.STOPPED,
                        Instant.parse("2026-04-13T10:00:00Z"),
                        Instant.parse("2026-04-13T10:10:00Z")
                ));

        graphQlTester.document("""
                mutation {
                  stopPlayback(input: {
                    playbackToken: "token-stop"
                    progressSeconds: 3400
                    durationSeconds: 3600
                    completed: true
                  }) {
                    id
                    episodeId
                    status
                  }
                }
                """)
                .execute()
                .path("stopPlayback.id").entity(String.class).isEqualTo("3")
                .path("stopPlayback.episodeId").entity(String.class).isEqualTo("9")
                .path("stopPlayback.status").entity(String.class).isEqualTo("STOPPED");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void resumePlaybackReturnsSessionPayload() {
        when(playbackService.resumePlayback(org.mockito.ArgumentMatchers.eq("member@example.com"), any()))
                .thenReturn(new PlaybackSessionPayload(
                        4L,
                        9L,
                        ContentType.SERIES,
                        77L,
                        8L,
                        9L,
                        "token-resume",
                        "https://stream.ott.local/playback/token-resume",
                        Instant.parse("2026-04-13T10:15:00Z"),
                        Instant.parse("2026-04-13T10:30:00Z"),
                        Instant.parse("2026-04-13T10:15:00Z"),
                        PlaybackSessionStatus.ACTIVE,
                        Instant.parse("2026-04-13T10:15:00Z"),
                        Instant.parse("2026-04-13T10:15:00Z")
                ));

        graphQlTester.document("""
                mutation {
                  resumePlayback(input: {
                    contentType: SERIES
                    contentId: "77"
                    seasonId: "8"
                    episodeId: "9"
                  }) {
                    id
                    contentType
                    episodeId
                    status
                  }
                }
                """)
                .execute()
                .path("resumePlayback.id").entity(String.class).isEqualTo("4")
                .path("resumePlayback.contentType").entity(String.class).isEqualTo("SERIES")
                .path("resumePlayback.episodeId").entity(String.class).isEqualTo("9")
                .path("resumePlayback.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void playbackAccessReturnsAvailabilityAndActiveSession() {
        when(playbackService.getPlaybackAccess(org.mockito.ArgumentMatchers.eq("member@example.com"), any()))
                .thenReturn(new PlaybackAccessPayload(
                        ContentType.MOVIE,
                        10L,
                        null,
                        null,
                        ContentAccessLevel.FREE,
                        true,
                        false,
                        "Playback available",
                        new PlaybackSessionPayload(
                                5L,
                                9L,
                                ContentType.MOVIE,
                                10L,
                                null,
                                null,
                                "token-active",
                                "https://stream.ott.local/playback/token-active",
                                Instant.parse("2026-04-13T10:00:00Z"),
                                Instant.parse("2026-04-13T10:15:00Z"),
                                Instant.parse("2026-04-13T10:05:00Z"),
                                PlaybackSessionStatus.ACTIVE,
                                Instant.parse("2026-04-13T10:00:00Z"),
                                Instant.parse("2026-04-13T10:05:00Z")
                        )
                ));

        graphQlTester.document("""
                query {
                  playbackAccess(input: {
                    contentType: MOVIE
                    contentId: "10"
                  }) {
                    accessLevel
                    allowed
                    requiresSubscription
                    reason
                    activeSession {
                      id
                      playbackToken
                    }
                  }
                }
                """)
                .execute()
                .path("playbackAccess.accessLevel").entity(String.class).isEqualTo("FREE")
                .path("playbackAccess.allowed").entity(Boolean.class).isEqualTo(true)
                .path("playbackAccess.requiresSubscription").entity(Boolean.class).isEqualTo(false)
                .path("playbackAccess.reason").entity(String.class).isEqualTo("Playback available")
                .path("playbackAccess.activeSession.id").entity(String.class).isEqualTo("5")
                .path("playbackAccess.activeSession.playbackToken").entity(String.class).isEqualTo("token-active");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void activePlaybackReturnsCurrentSession() {
        when(playbackService.getActivePlayback(org.mockito.ArgumentMatchers.eq("member@example.com"), any()))
                .thenReturn(new PlaybackSessionPayload(
                        6L,
                        9L,
                        ContentType.SERIES,
                        77L,
                        8L,
                        9L,
                        "token-current",
                        "https://stream.ott.local/playback/token-current",
                        Instant.parse("2026-04-13T10:15:00Z"),
                        Instant.parse("2026-04-13T10:30:00Z"),
                        Instant.parse("2026-04-13T10:20:00Z"),
                        PlaybackSessionStatus.ACTIVE,
                        Instant.parse("2026-04-13T10:15:00Z"),
                        Instant.parse("2026-04-13T10:20:00Z")
                ));

        graphQlTester.document("""
                query {
                  activePlayback(input: {
                    contentType: SERIES
                    contentId: "77"
                    seasonId: "8"
                    episodeId: "9"
                  }) {
                    id
                    contentType
                    episodeId
                    status
                  }
                }
                """)
                .execute()
                .path("activePlayback.id").entity(String.class).isEqualTo("6")
                .path("activePlayback.contentType").entity(String.class).isEqualTo("SERIES")
                .path("activePlayback.episodeId").entity(String.class).isEqualTo("9")
                .path("activePlayback.status").entity(String.class).isEqualTo("ACTIVE");
    }
}
