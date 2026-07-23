package com.colife.api.subscription.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {
    /** Inscription active : pas de date de désinscription */
    boolean existsByActivityIdAndUserIdAndUnsubscribedAtIsNull(Long activityId, UUID userId);

    Optional<SubscriptionEntity> findByActivityIdAndUserId(Long activityId, UUID userId);

    @Query("select count(s) from SubscriptionEntity s where s.activityId = :activityId and s.unsubscribedAt is null")
    int countActiveByActivityId(@Param("activityId") Long activityId);

    @Query("select s.userId from SubscriptionEntity s where s.activityId = :activityId and s.unsubscribedAt is null")
    List<UUID> findUserIdsByActivityId(@Param("activityId") Long activityId);

    /**
     * Vérifie via une requête native si l'un des utilisateurs est inscrit à une autre activité
     * (hors excludeActivityId) sur le même créneau horaire.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM subscriptions s
            INNER JOIN activities a ON s.activity_id = a.id
            WHERE s.user_id IN :userIds
              AND s.unsubscribed_at IS NULL
              AND a.is_deleted = false
              AND a.id <> :excludeActivityId
              AND a.date = :date
              AND a.start_time < :endTime
              AND a.end_time > :startTime
            """, nativeQuery = true)
    int countConflictingActivityForSubscribedUsers(
            @Param("userIds") List<UUID> userIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeActivityId") Long excludeActivityId);

    void deleteByActivityId(Long activityId);
}