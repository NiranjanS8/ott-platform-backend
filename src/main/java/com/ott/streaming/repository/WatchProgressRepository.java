package com.ott.streaming.repository;

import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.WatchProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchProgressRepository extends JpaRepository<WatchProgress, Long> {

    Optional<WatchProgress> findByUserIdAndContentTypeAndContentIdAndEpisodeIdIsNull(
            Long userId,
            ContentType contentType,
            Long contentId
    );

    Optional<WatchProgress> findByUserIdAndContentTypeAndContentIdAndEpisodeId(
            Long userId,
            ContentType contentType,
            Long contentId,
            Long episodeId
    );

    List<WatchProgress> findByUserIdOrderByLastWatchedAtDesc(Long userId);
}
