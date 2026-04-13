package com.ott.streaming.dto.playback;

import com.ott.streaming.entity.ContentAccessLevel;

public record ResolvedPlaybackTarget(
        Long seasonId,
        Long episodeId,
        ContentAccessLevel accessLevel
) {
}
