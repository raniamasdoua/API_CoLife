package com.colife.api.subscription.infrastructure;

import org.springframework.stereotype.Component;

import com.colife.api.subscription.domain.SubscriptionRepositoryPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {
    private final SubscriptionJpaRepository jpa;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByActivityIdAndUserId(Long activityId, UUID userId) {
        return jpa.existsByActivityIdAndUserIdAndUnsubscribedAtIsNull(activityId, userId);
    }

    @Override
    public void registerParticipant(Long activityId, UUID userId) {
        jpa.findByActivityIdAndUserId(activityId, userId).ifPresentOrElse(
                entity -> {
                    if (entity.getUnsubscribedAt() == null) {
                        throw new IllegalStateException("Inscription déjà active");
                    }
                    entity.setUnsubscribedAt(null);
                    entity.setSubscribedAt(LocalDateTime.now());
                    jpa.save(entity);
                },
                () -> {
                    SubscriptionEntity entity = new SubscriptionEntity();
                    entity.setActivityId(activityId);
                    entity.setUserId(userId);
                    entity.setSubscribedAt(LocalDateTime.now());
                    entity.setUnsubscribedAt(null);
                    jpa.save(entity);
                }
        );
    }

    @Override
    public void unsubscribeParticipant(Long activityId, UUID userId) {
        SubscriptionEntity entity = jpa.findByActivityIdAndUserId(activityId, userId)
                .orElseThrow(() -> new IllegalStateException("Aucune inscription pour cette activité"));
        if (entity.getUnsubscribedAt() != null) {
            throw new IllegalStateException("Inscription déjà annulée");
        }
        entity.setUnsubscribedAt(LocalDateTime.now());
        jpa.save(entity);
    }

    @Override
    public int countParticipants(Long activityId) {
        return jpa.countActiveByActivityId(activityId);
    }

    @Override
    public List<UUID> findUserIdsByActivityId(Long activityId) {
        return jpa.findUserIdsByActivityId(activityId);
    }

    @Override
    public boolean existsConflictingActivityForSubscribedUsers(
            List<UUID> userIds, LocalDate date, LocalTime start, LocalTime end, Long excludeActivityId) {
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        return jpa.countConflictingActivityForSubscribedUsers(userIds, date, start, end, excludeActivityId) > 0;
    }

    @Override
    public void deleteAllByActivityId(Long activityId) {
        jpa.deleteByActivityId(activityId);
    }
}
