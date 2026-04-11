package com.ott.streaming.dto.discovery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CatalogQueryInput(
        @Size(max = 100, message = "Search text must be at most 100 characters")
        String search,
        @Valid CatalogFilterInput filter,
        @NotNull(message = "Sort option is required")
        CatalogSortOption sort,
        @Valid @NotNull(message = "Pagination is required")
        PaginationInput pagination
) {
}
