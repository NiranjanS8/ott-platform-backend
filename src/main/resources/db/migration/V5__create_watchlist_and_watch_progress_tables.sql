CREATE TABLE watchlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_watchlist_content_type CHECK (content_type IN ('MOVIE', 'SERIES')),
    CONSTRAINT uk_watchlist_user_content UNIQUE (user_id, content_type, content_id)
);

CREATE INDEX idx_watchlist_user_created_at ON watchlist_items (user_id, created_at DESC);

CREATE TABLE watch_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    season_id BIGINT REFERENCES seasons(id) ON DELETE CASCADE,
    episode_id BIGINT REFERENCES episodes(id) ON DELETE CASCADE,
    progress_seconds INT NOT NULL DEFAULT 0,
    duration_seconds INT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    last_watched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_watch_progress_content_type CHECK (content_type IN ('MOVIE', 'SERIES')),
    CONSTRAINT chk_watch_progress_non_negative CHECK (progress_seconds >= 0 AND duration_seconds > 0),
    CONSTRAINT chk_watch_progress_bounds CHECK (progress_seconds <= duration_seconds),
    CONSTRAINT chk_watch_progress_movie_shape CHECK (
        (content_type = 'MOVIE' AND season_id IS NULL AND episode_id IS NULL)
        OR content_type = 'SERIES'
    ),
    CONSTRAINT chk_watch_progress_episode_shape CHECK (
        (content_type = 'SERIES' AND episode_id IS NOT NULL)
        OR content_type = 'MOVIE'
    )
);

CREATE UNIQUE INDEX uk_watch_progress_movie
    ON watch_progress (user_id, content_type, content_id)
    WHERE episode_id IS NULL;

CREATE UNIQUE INDEX uk_watch_progress_episode
    ON watch_progress (user_id, content_type, content_id, episode_id)
    WHERE episode_id IS NOT NULL;

CREATE INDEX idx_watch_progress_user_last_watched
    ON watch_progress (user_id, last_watched_at DESC);
