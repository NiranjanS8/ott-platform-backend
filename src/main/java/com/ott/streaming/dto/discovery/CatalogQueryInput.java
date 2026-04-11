package com.ott.streaming.dto.discovery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CatalogQueryInput(
        String search,
        @Valid CatalogFilterInput filter,
        @NotNull(message = "Sort option is required")
        CatalogSortOption sort,
        @Valid @NotNull(message = "Pagination is required")
        PaginationInput pagination
) {
}
