package com.example.api.subscription.domain;

import java.util.List;

public interface SubscriptionRepositoryPort {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
}
