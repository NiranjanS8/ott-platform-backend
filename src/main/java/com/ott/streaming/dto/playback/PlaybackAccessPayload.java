package com.ott.streaming.dto.playback;

import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;

public record PlaybackAccessPayload(
        ContentType contentType,
        Long contentId,
        Long seasonId,
        Long episodeId,
        ContentAccessLevel accessLevel,
        boolean allowed,
        boolean requiresSubscription,
        String reason,
        PlaybackSessionPayload activeSession
) {
}
