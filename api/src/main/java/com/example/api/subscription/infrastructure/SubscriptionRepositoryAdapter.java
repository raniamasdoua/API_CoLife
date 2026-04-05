package com.example.api.subscription.infrastructure;

import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    @Override
    public void registerParticipant(Long activityId, Long userId) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setActivityId(activityId);
        entity.setUserId(userId);
        entity.setSubscribedAt(LocalDateTime.now());
        jpa.save(entity);
    }

    @Override
    public int countParticipants(Long activityId) {
        return jpa.countByActivityId(activityId);
    }
}
