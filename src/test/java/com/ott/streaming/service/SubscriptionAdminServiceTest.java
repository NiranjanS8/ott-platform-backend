package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.subscription.CreateSubscriptionPlanInput;
import com.ott.streaming.dto.subscription.UpdateContentAccessInput;
import com.ott.streaming.dto.subscription.UpdateSubscriptionPlanInput;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Series;
import com.ott.streaming.entity.SubscriptionPlan;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.SubscriptionPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionAdminServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeriesRepository seriesRepository;

    private SubscriptionAdminService subscriptionAdminService;

    @BeforeEach
    void setUp() {
        subscriptionAdminService = new SubscriptionAdminService(
                subscriptionPlanRepository,
                movieRepository,
                seriesRepository
        );
    }

    @Test
    void createSubscriptionPlanNormalizesNameAndRejectsDuplicates() {
        when(subscriptionPlanRepository.existsByNameIgnoreCase("Premium Monthly")).thenReturn(false);
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenAnswer(invocation -> {
            SubscriptionPlan plan = invocation.getArgument(0);
            plan.setId(1L);
            ReflectionTestUtils.setField(plan, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
            ReflectionTestUtils.setField(plan, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
            return plan;
        });

        var payload = subscriptionAdminService.createSubscriptionPlan(new CreateSubscriptionPlanInput(
                "  Premium Monthly  ",
                "  Access for 30 days  ",
                new BigDecimal("9.99"),
                30,
                true
        ));

        assertThat(payload.id()).isEqualTo(1L);
        assertThat(payload.name()).isEqualTo("Premium Monthly");
        assertThat(payload.description()).isEqualTo("Access for 30 days");
        assertThat(payload.active()).isTrue();
    }

    @Test
    void createSubscriptionPlanThrowsWhenNameAlreadyExists() {
        when(subscriptionPlanRepository.existsByNameIgnoreCase("Premium Monthly")).thenReturn(true);

        assertThatThrownBy(() -> subscriptionAdminService.createSubscriptionPlan(new CreateSubscriptionPlanInput(
                "Premium Monthly",
                null,
                new BigDecimal("9.99"),
                30,
                true
        )))
                .isInstanceOf(ApiException.class)
                .hasMessage("Subscription plan already exists");
    }

    @Test
    void createSubscriptionPlanTranslatesConstraintRaceToDuplicateError() {
        when(subscriptionPlanRepository.existsByNameIgnoreCase("Premium Monthly")).thenReturn(false);
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class)))
                .thenThrow(new DataIntegrityViolationException("subscription_plans_name_key"));

        assertThatThrownBy(() -> subscriptionAdminService.createSubscriptionPlan(new CreateSubscriptionPlanInput(
                "Premium Monthly",
                null,
                new BigDecimal("9.99"),
                30,
                true
        )))
                .isInstanceOf(ApiException.class)
                .hasMessage("Subscription plan already exists");
    }

    @Test
    void updateSubscriptionPlanRejectsMissingPlan() {
        when(subscriptionPlanRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionAdminService.updateSubscriptionPlan(9L, new UpdateSubscriptionPlanInput(
                "Premium Yearly",
                null,
                new BigDecimal("99.99"),
                365,
                true
        )))
                .isInstanceOf(ApiException.class)
                .hasMessage("Subscription plan not found");
    }

    @Test
    void updateMovieAccessLevelPersistsPremiumAccess() {
        Movie movie = new Movie();
        movie.setId(10L);
        movie.setTitle("The Matrix");
        movie.setReleaseDate(LocalDate.parse("1999-03-31"));
        movie.setMaturityRating("R");
        movie.setAccessLevel(ContentAccessLevel.FREE);
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(movieRepository.save(movie)).thenAnswer(invocation -> invocation.getArgument(0));

        var payload = subscriptionAdminService.updateMovieAccessLevel(
                10L,
                new UpdateContentAccessInput(ContentAccessLevel.PREMIUM)
        );

        assertThat(payload.accessLevel()).isEqualTo(ContentAccessLevel.PREMIUM);
    }

    @Test
    void updateSeriesAccessLevelRejectsMissingSeries() {
        when(seriesRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionAdminService.updateSeriesAccessLevel(
                4L,
                new UpdateContentAccessInput(ContentAccessLevel.PREMIUM)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Series not found");
    }
}
