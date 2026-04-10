CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_review_content_type CHECK (content_type IN ('MOVIE', 'SERIES')),
    CONSTRAINT chk_review_rating_range CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uk_review_user_content UNIQUE (user_id, content_type, content_id)
);

CREATE INDEX idx_reviews_content_lookup ON reviews (content_type, content_id);
