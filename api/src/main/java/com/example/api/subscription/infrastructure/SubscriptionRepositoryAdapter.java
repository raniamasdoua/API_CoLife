package com.example.api.subscription.infrastructure;

import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    @Override
    public List<Long> findUserIdsByActivityId(Long activityId) {
        return jpa.findUserIdsByActivityId(activityId);
    }

    @Override
    public boolean existsConflictingActivityForSubscribedUsers(
            List<Long> userIds, LocalDate date, LocalTime start, LocalTime end, Long excludeActivityId) {
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
