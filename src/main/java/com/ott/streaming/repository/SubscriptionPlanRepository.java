package com.ott.streaming.repository;

import com.ott.streaming.entity.SubscriptionPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<SubscriptionPlan> findByNameIgnoreCase(String name);
}
