package com.ott.streaming.dto.discovery;

import java.util.List;

public record CatalogPagePayload(
        List<CatalogItemPayload> items,
        PaginationInfoPayload pageInfo
) {
}
