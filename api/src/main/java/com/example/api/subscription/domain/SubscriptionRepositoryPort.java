package com.example.api.subscription.domain;

public interface SubscriptionRepositoryPort {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);

    void registerParticipant(Long activityId, Long userId);

    int countParticipants(Long activityId);
}
