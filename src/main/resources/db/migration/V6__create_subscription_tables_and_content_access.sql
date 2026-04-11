ALTER TABLE movies
    ADD COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD CONSTRAINT chk_movie_access_level CHECK (access_level IN ('FREE', 'PREMIUM'));

ALTER TABLE series
    ADD COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD CONSTRAINT chk_series_access_level CHECK (access_level IN ('FREE', 'PREMIUM'));

CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    duration_days INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscription_plan_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_subscription_plan_duration_positive CHECK (duration_days > 0)
);

CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_subscription_status CHECK (status IN ('ACTIVE', 'CANCELED', 'EXPIRED')),
    CONSTRAINT chk_user_subscription_date_range CHECK (end_date > start_date)
);

CREATE INDEX idx_user_subscriptions_user_created_at
    ON user_subscriptions (user_id, created_at DESC);

CREATE INDEX idx_user_subscriptions_user_status_end_date
    ON user_subscriptions (user_id, status, end_date DESC);
