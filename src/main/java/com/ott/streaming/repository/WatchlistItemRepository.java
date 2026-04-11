package com.ott.streaming.repository;

import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.WatchlistItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    boolean existsByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, Long contentId);

    Optional<WatchlistItem> findByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, Long contentId);
}
