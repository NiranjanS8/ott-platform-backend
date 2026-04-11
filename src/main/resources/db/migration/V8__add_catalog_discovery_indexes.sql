CREATE INDEX idx_movies_access_release_date
    ON movies (access_level, release_date);

CREATE INDEX idx_movies_language
    ON movies (language);

CREATE INDEX idx_movies_lower_title
    ON movies ((LOWER(title)));

CREATE INDEX idx_movies_lower_language
    ON movies ((LOWER(language)));

CREATE INDEX idx_movie_genres_genre_movie
    ON movie_genres (genre_id, movie_id);

CREATE INDEX idx_series_access_release_date
    ON series (access_level, release_date);

CREATE INDEX idx_series_language
    ON series (language);

CREATE INDEX idx_series_lower_title
    ON series ((LOWER(title)));

CREATE INDEX idx_series_lower_language
    ON series ((LOWER(language)));

CREATE INDEX idx_series_genres_genre_series
    ON series_genres (genre_id, series_id);
