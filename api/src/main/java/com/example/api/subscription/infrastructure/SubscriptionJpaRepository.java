package com.example.api.subscription.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
}