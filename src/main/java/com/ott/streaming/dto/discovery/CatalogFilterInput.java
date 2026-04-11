package com.ott.streaming.dto.discovery;

import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;

public record CatalogFilterInput(
        Long genreId,
        String language,
        Integer releaseYear,
        Double minRating,
        Double maxRating,
        ContentType contentType,
        ContentAccessLevel accessLevel
) {
}
