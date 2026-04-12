package com.ott.streaming.service;

import com.ott.streaming.dto.subscription.SubscribeToPlanInput;
import com.ott.streaming.dto.subscription.UserSubscriptionPayload;
import com.ott.streaming.entity.SubscriptionPlan;
import com.ott.streaming.entity.SubscriptionStatus;
import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.entity.UserSubscription;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.SubscriptionPlanRepository;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.repository.UserSubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Service;

@Service
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;

    public UserSubscriptionService(UserSubscriptionRepository userSubscriptionRepository,
                                   SubscriptionPlanRepository subscriptionPlanRepository,
                                   UserRepository userRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.userRepository = userRepository;
    }

    public UserSubscriptionPayload subscribeToPlan(String email, SubscribeToPlanInput input) {
        User currentUser = getAuthenticatedUser(email);
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.planId())
                .orElseThrow(() -> ApiException.notFound("Subscription plan not found"));

        if (!plan.isActive()) {
            throw ApiException.validation("Subscription plan is not active");
        }

        closeExistingActiveSubscription(currentUser.getId());

        Instant startDate = Instant.now();
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(currentUser.getId());
        subscription.setPlanId(plan.getId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(startDate);
        subscription.setEndDate(startDate.plus(plan.getDurationDays(), ChronoUnit.DAYS));

        return toPayload(userSubscriptionRepository.save(subscription));
    }

    public UserSubscriptionPayload getCurrentSubscription(String email) {
        User currentUser = getAuthenticatedUser(email);
        return userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(
                        currentUser.getId(),
                        SubscriptionStatus.ACTIVE
                )
                .map(this::expireIfNeeded)
                .map(this::toPayload)
                .orElse(null);
    }

    public boolean hasPremiumAccess(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return userRepository.findByEmail(normalizeEmail(email))
                .map(user -> {
                    if (user.getRole() == Role.ADMIN) {
                        return true;
                    }

                    return userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(
                                    user.getId(),
                                    SubscriptionStatus.ACTIVE
                            )
                            .map(this::expireIfNeeded)
                            .isPresent();
                })
                .orElse(false);
    }

    private void closeExistingActiveSubscription(Long userId) {
        userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    Instant now = Instant.now();
                    existing.setStatus(existing.getEndDate().isAfter(now)
                            ? SubscriptionStatus.CANCELED
                            : SubscriptionStatus.EXPIRED);
                    userSubscriptionRepository.save(existing);
                });
    }

    private UserSubscription expireIfNeeded(UserSubscription subscription) {
        if (subscription.getEndDate().isAfter(Instant.now())) {
            return subscription;
        }

        subscription.setStatus(SubscriptionStatus.EXPIRED);
        userSubscriptionRepository.save(subscription);
        return null;
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.unauthorized("Authentication is required");
        }

        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> ApiException.unauthorized("Authenticated user not found"));
    }

    private UserSubscriptionPayload toPayload(UserSubscription subscription) {
        return new UserSubscriptionPayload(
                subscription.getId(),
                subscription.getUserId(),
                subscription.getPlanId(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
