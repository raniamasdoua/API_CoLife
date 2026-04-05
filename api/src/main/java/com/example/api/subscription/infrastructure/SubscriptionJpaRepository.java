package com.example.api.subscription.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);

    int countByActivityId(Long activityId);

    @Query("select s.userId from SubscriptionEntity s where s.activityId = :activityId")
    List<Long> findUserIdsByActivityId(@Param("activityId") Long activityId);

    /**
     * Vérifie via une requête native si l'un des utilisateurs est inscrit à une autre activité
     * (hors excludeActivityId) sur le même créneau horaire.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM subscriptions s
            INNER JOIN activities a ON s.activity_id = a.id
            WHERE s.user_id IN :userIds
              AND a.is_deleted = false
              AND a.id <> :excludeActivityId
              AND a.date = :date
              AND a.start_time < :endTime
              AND a.end_time > :startTime
            """, nativeQuery = true)
    int countConflictingActivityForSubscribedUsers(
            @Param("userIds") List<Long> userIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeActivityId") Long excludeActivityId);

    void deleteByActivityId(Long activityId);
}