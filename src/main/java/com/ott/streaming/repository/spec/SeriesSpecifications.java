package com.ott.streaming.repository.spec;

import com.ott.streaming.dto.discovery.CatalogFilterInput;
import com.ott.streaming.entity.Series;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class SeriesSpecifications {

    private SeriesSpecifications() {
    }

    public static Specification<Series> forCatalog(String search, CatalogFilterInput filter) {
        return Specification.where(titleContains(search))
                .and(hasAccessLevel(filter))
                .and(releasedInYear(filter))
                .and(hasLanguage(filter))
                .and(hasGenre(filter));
    }

    private static Specification<Series> titleContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }

    private static Specification<Series> hasAccessLevel(CatalogFilterInput filter) {
        if (filter == null || filter.accessLevel() == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("accessLevel"), filter.accessLevel());
    }

    private static Specification<Series> releasedInYear(CatalogFilterInput filter) {
        if (filter == null || filter.releaseYear() == null) {
            return null;
        }
        LocalDate start = LocalDate.of(filter.releaseYear(), 1, 1);
        LocalDate end = start.plusYears(1);
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("releaseDate")),
                cb.greaterThanOrEqualTo(root.get("releaseDate"), start),
                cb.lessThan(root.get("releaseDate"), end)
        );
    }

    private static Specification<Series> hasLanguage(CatalogFilterInput filter) {
        if (filter == null || filter.language() == null || filter.language().isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("language")), filter.language().trim().toLowerCase());
    }

    private static Specification<Series> hasGenre(CatalogFilterInput filter) {
        if (filter == null || filter.genreId() == null) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("genres").get("id"), filter.genreId());
        };
    }
}
