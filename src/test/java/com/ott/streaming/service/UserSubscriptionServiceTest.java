package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.subscription.SubscribeToPlanInput;
import com.ott.streaming.dto.subscription.UserSubscriptionPayload;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.SubscriptionPlan;
import com.ott.streaming.entity.SubscriptionStatus;
import com.ott.streaming.entity.User;
import com.ott.streaming.entity.UserSubscription;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.SubscriptionPlanRepository;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.repository.UserSubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private UserRepository userRepository;

    private UserSubscriptionService userSubscriptionService;

    @BeforeEach
    void setUp() {
        userSubscriptionService = new UserSubscriptionService(
                userSubscriptionRepository,
                subscriptionPlanRepository,
                userRepository
        );
    }

    @Test
    void subscribeToPlanCreatesActiveSubscriptionForAuthenticatedUser() {
        User user = existingUser();
        SubscriptionPlan plan = activePlan();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(5L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> {
            UserSubscription saved = invocation.getArgument(0);
            saved.setId(20L);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
            ReflectionTestUtils.setField(saved, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));
            return saved;
        });

        UserSubscriptionPayload payload = userSubscriptionService.subscribeToPlan(
                " Member@Example.com ",
                new SubscribeToPlanInput(7L)
        );

        assertThat(payload.id()).isEqualTo(20L);
        assertThat(payload.userId()).isEqualTo(5L);
        assertThat(payload.planId()).isEqualTo(7L);
        assertThat(payload.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(payload.endDate()).isAfter(payload.startDate());
    }

    @Test
    void subscribeToPlanCancelsExistingActiveSubscriptionBeforeCreatingNewOne() {
        User user = existingUser();
        SubscriptionPlan plan = activePlan();
        UserSubscription existing = new UserSubscription();
        existing.setId(10L);
        existing.setUserId(5L);
        existing.setPlanId(2L);
        existing.setStatus(SubscriptionStatus.ACTIVE);
        existing.setStartDate(Instant.now().minus(2, ChronoUnit.DAYS));
        existing.setEndDate(Instant.now().plus(28, ChronoUnit.DAYS));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(5L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userSubscriptionService.subscribeToPlan("member@example.com", new SubscribeToPlanInput(7L));

        assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(userSubscriptionRepository).save(existing);
    }

    @Test
    void subscribeToPlanRejectsInactivePlan() {
        User user = existingUser();
        SubscriptionPlan plan = activePlan();
        plan.setActive(false);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.findById(7L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> userSubscriptionService.subscribeToPlan("member@example.com", new SubscribeToPlanInput(7L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Subscription plan is not active");
    }

    @Test
    void currentSubscriptionReturnsActiveSubscription() {
        User user = existingUser();
        UserSubscription subscription = new UserSubscription();
        subscription.setId(25L);
        subscription.setUserId(5L);
        subscription.setPlanId(7L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.parse("2026-04-10T10:00:00Z"));
        subscription.setEndDate(Instant.now().plus(2, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(subscription, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(5L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        UserSubscriptionPayload payload = userSubscriptionService.getCurrentSubscription("member@example.com");

        assertThat(payload).isNotNull();
        assertThat(payload.id()).isEqualTo(25L);
        assertThat(payload.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(userSubscriptionRepository, never()).save(subscription);
    }

    @Test
    void currentSubscriptionExpiresPastDueSubscriptionAndReturnsNull() {
        User user = existingUser();
        UserSubscription subscription = new UserSubscription();
        subscription.setId(26L);
        subscription.setUserId(5L);
        subscription.setPlanId(7L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.parse("2026-04-01T10:00:00Z"));
        subscription.setEndDate(Instant.now().minus(1, ChronoUnit.HOURS));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(5L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSubscriptionPayload payload = userSubscriptionService.getCurrentSubscription("member@example.com");

        assertThat(payload).isNull();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(userSubscriptionRepository).save(subscription);
    }

    private User existingUser() {
        User user = new User();
        user.setId(5L);
        user.setName("Member");
        user.setEmail("member@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-04-09T12:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-04-09T12:00:00Z"));
        return user;
    }

    private SubscriptionPlan activePlan() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(7L);
        plan.setName("Premium Monthly");
        plan.setDescription("Access for 30 days");
        plan.setPrice(new BigDecimal("9.99"));
        plan.setDurationDays(30);
        plan.setActive(true);
        return plan;
    }
}
