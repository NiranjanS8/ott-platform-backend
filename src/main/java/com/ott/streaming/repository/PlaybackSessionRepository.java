package com.ott.streaming.repository;

import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.PlaybackSession;
import com.ott.streaming.entity.PlaybackSessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybackSessionRepository extends JpaRepository<PlaybackSession, Long> {

    Optional<PlaybackSession> findByPlaybackToken(String playbackToken);

    List<PlaybackSession> findByUserIdAndStatusOrderByLastHeartbeatAtDesc(Long userId, PlaybackSessionStatus status);

    Optional<PlaybackSession> findFirstByUserIdAndContentTypeAndContentIdAndStatusOrderByStartedAtDesc(
            Long userId,
            ContentType contentType,
            Long contentId,
            PlaybackSessionStatus status
    );

    Optional<PlaybackSession> findFirstByUserIdAndContentTypeAndContentIdAndEpisodeIdAndStatusOrderByStartedAtDesc(
            Long userId,
            ContentType contentType,
            Long contentId,
            Long episodeId,
            PlaybackSessionStatus status
    );
}
