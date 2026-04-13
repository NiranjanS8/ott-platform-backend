CREATE TABLE playback_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    season_id BIGINT REFERENCES seasons(id) ON DELETE CASCADE,
    episode_id BIGINT REFERENCES episodes(id) ON DELETE CASCADE,
    playback_token VARCHAR(128) NOT NULL UNIQUE,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_heartbeat_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_playback_content_type CHECK (content_type IN ('MOVIE', 'SERIES')),
    CONSTRAINT chk_playback_status CHECK (status IN ('ACTIVE', 'STOPPED', 'EXPIRED')),
    CONSTRAINT chk_playback_expiry_window CHECK (expires_at >= started_at),
    CONSTRAINT chk_playback_heartbeat_window CHECK (last_heartbeat_at >= started_at),
    CONSTRAINT chk_playback_movie_shape CHECK (
        (content_type = 'MOVIE' AND season_id IS NULL AND episode_id IS NULL)
        OR content_type = 'SERIES'
    ),
    CONSTRAINT chk_playback_episode_shape CHECK (
        (content_type = 'SERIES' AND episode_id IS NOT NULL)
        OR content_type = 'MOVIE'
    )
);

CREATE INDEX idx_playback_sessions_user_status_heartbeat
    ON playback_sessions (user_id, status, last_heartbeat_at DESC);

CREATE INDEX idx_playback_sessions_content_status
    ON playback_sessions (content_type, content_id, status);

CREATE INDEX idx_playback_sessions_expires_at
    ON playback_sessions (expires_at);
