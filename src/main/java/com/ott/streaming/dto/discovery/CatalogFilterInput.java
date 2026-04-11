package com.ott.streaming.dto.discovery;

import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Year;

public record CatalogFilterInput(
        Long genreId,
        @Size(max = 100, message = "Language must be at most 100 characters")
        String language,
        @Min(value = 1888, message = "Release year must be 1888 or later")
        @Max(value = 2100, message = "Release year must not exceed 2100")
        Integer releaseYear,
        @DecimalMin(value = "1.0", message = "Minimum rating must be at least 1.0")
        @DecimalMax(value = "5.0", message = "Minimum rating must not exceed 5.0")
        Double minRating,
        @DecimalMin(value = "1.0", message = "Maximum rating must be at least 1.0")
        @DecimalMax(value = "5.0", message = "Maximum rating must not exceed 5.0")
        Double maxRating,
        ContentType contentType,
        ContentAccessLevel accessLevel
) {

    @AssertTrue(message = "Minimum rating must be less than or equal to maximum rating")
    public boolean isRatingRangeValid() {
        return minRating == null || maxRating == null || minRating <= maxRating;
    }

    @AssertTrue(message = "Release year must not be in the far future")
    public boolean isReleaseYearNotTooFarAhead() {
        return releaseYear == null || releaseYear <= Year.now().getValue() + 1;
    }
}
