package com.ott.streaming.dto.playback;

import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.PlaybackSessionStatus;
import java.time.Instant;

public record PlaybackSessionPayload(
        Long id,
        Long userId,
        ContentType contentType,
        Long contentId,
        Long seasonId,
        Long episodeId,
        String playbackToken,
        String streamUrl,
        Instant startedAt,
        Instant expiresAt,
        Instant lastHeartbeatAt,
        PlaybackSessionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
