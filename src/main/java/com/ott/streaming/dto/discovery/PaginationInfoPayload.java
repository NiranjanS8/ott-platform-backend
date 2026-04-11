package com.ott.streaming.dto.discovery;

public record PaginationInfoPayload(
        int page,
        int size,
        int totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
