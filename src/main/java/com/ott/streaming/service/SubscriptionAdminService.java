package com.ott.streaming.service;

import com.ott.streaming.config.CacheNames;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.subscription.CreateSubscriptionPlanInput;
import com.ott.streaming.dto.subscription.SubscriptionPlanPayload;
import com.ott.streaming.dto.subscription.UpdateContentAccessInput;
import com.ott.streaming.dto.subscription.UpdateSubscriptionPlanInput;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Series;
import com.ott.streaming.entity.SubscriptionPlan;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.SubscriptionPlanRepository;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionAdminService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    public SubscriptionAdminService(SubscriptionPlanRepository subscriptionPlanRepository,
                                    MovieRepository movieRepository,
                                    SeriesRepository seriesRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionPlanPayload createSubscriptionPlan(CreateSubscriptionPlanInput input) {
        String normalizedName = normalizeName(input.name());
        if (subscriptionPlanRepository.existsByNameIgnoreCase(normalizedName)) {
            throw ApiException.duplicateResource("Subscription plan already exists");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        applyPlanInput(plan, normalizedName, input.description(), input.price(), input.durationDays(), input.active());
        try {
            return toPlanPayload(subscriptionPlanRepository.save(plan));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.duplicateResource("Subscription plan already exists");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionPlanPayload updateSubscriptionPlan(Long id, UpdateSubscriptionPlanInput input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Subscription plan not found"));

        String normalizedName = normalizeName(input.name());
        subscriptionPlanRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.duplicateResource("Subscription plan already exists");
                });

        applyPlanInput(plan, normalizedName, input.description(), input.price(), input.durationDays(), input.active());
        try {
            return toPlanPayload(subscriptionPlanRepository.save(plan));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.duplicateResource("Subscription plan already exists");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CONTENT_MOVIES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CONTENT_MOVIE_BY_ID, key = "#id")
    })
    public MoviePayload updateMovieAccessLevel(Long id, UpdateContentAccessInput input) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Movie not found"));

        movie.setAccessLevel(input.accessLevel());
        return toMoviePayload(movieRepository.save(movie));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CONTENT_SERIES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CONTENT_SERIES_BY_ID, key = "#id")
    })
    public SeriesPayload updateSeriesAccessLevel(Long id, UpdateContentAccessInput input) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Series not found"));

        series.setAccessLevel(input.accessLevel());
        return toSeriesPayload(seriesRepository.save(series));
    }

    private void applyPlanInput(SubscriptionPlan plan,
                                String normalizedName,
                                String description,
                                java.math.BigDecimal price,
                                Integer durationDays,
                                Boolean active) {
        plan.setName(normalizedName);
        plan.setDescription(normalizeOptionalText(description));
        plan.setPrice(price);
        plan.setDurationDays(durationDays);
        plan.setActive(Boolean.TRUE.equals(active));
    }

    private SubscriptionPlanPayload toPlanPayload(SubscriptionPlan plan) {
        return new SubscriptionPlanPayload(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.isActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private MoviePayload toMoviePayload(Movie movie) {
        return new MoviePayload(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getReleaseDate() == null ? null : movie.getReleaseDate().toString(),
                movie.getDurationMinutes(),
                movie.getMaturityRating(),
                movie.getLanguage(),
                movie.getAccessLevel(),
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }

    private SeriesPayload toSeriesPayload(Series series) {
        return new SeriesPayload(
                series.getId(),
                series.getTitle(),
                series.getDescription(),
                series.getReleaseDate() == null ? null : series.getReleaseDate().toString(),
                series.getEndDate() == null ? null : series.getEndDate().toString(),
                series.getMaturityRating(),
                series.getLanguage(),
                series.getAccessLevel(),
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
