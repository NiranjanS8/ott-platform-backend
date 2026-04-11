package com.ott.streaming.dto.discovery;

import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;

public record CatalogItemPayload(
        Long id,
        ContentType contentType,
        String title,
        String description,
        String releaseDate,
        String endDate,
        String maturityRating,
        ContentAccessLevel accessLevel,
        Double averageRating
) {
}
