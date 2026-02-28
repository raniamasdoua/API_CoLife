package com.example.api.subscription.infrastructure;

import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {
    private final SubscriptionJpaRepository jpa;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByActivityIdAndUserId(Long activityId, Long userId) {
        return jpa.existsByActivityIdAndUserId(activityId, userId);
    }


}
