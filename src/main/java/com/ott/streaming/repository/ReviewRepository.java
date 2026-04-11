package com.ott.streaming.repository;

import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, Long contentId);

    List<Review> findByContentTypeAndContentIdOrderByCreatedAtDesc(ContentType contentType, Long contentId);

    List<Review> findByContentTypeAndContentId(ContentType contentType, Long contentId);
}
