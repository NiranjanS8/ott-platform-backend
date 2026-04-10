package com.ott.streaming.dto.review;

public record RatingSummaryPayload(
        Double averageRating,
        Integer reviewCount
) {
}
