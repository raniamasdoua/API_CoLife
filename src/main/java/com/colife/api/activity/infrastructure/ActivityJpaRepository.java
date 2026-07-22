package com.colife.api.activity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, Long> {

    @Query("""
            select a from ActivityEntity a
            join fetch a.type
            where a.organizer.id = :organizerId
              and a.isDeleted = false
            """)
    List<ActivityEntity> findByOrganizerIdAndNotDeleted(@Param("organizerId") UUID organizerId);

    @Query("""
            select a from ActivityEntity a
            join fetch a.type
            where a.organizer.id <> :organizerId
              and a.isDeleted = false
            """)
    List<ActivityEntity> findByOrganizerIdNotAndNotDeleted(@Param("organizerId") UUID organizerId);

    /**
     * Activités "à découvrir" pour un utilisateur : pas les siennes en tant qu'organisateur,
     * non supprimées, pas déjà inscrit, et encore ouvertes à l'inscription (pas passées / pas commencées).
     */
    @Query("""
            select a from ActivityEntity a
            join fetch a.type
            where a.organizer.id <> :userId
              and a.isDeleted = false
              and a.id not in (select s.activityId from SubscriptionEntity s where s.userId = :userId and s.unsubscribedAt is null)
              and (
                    a.date > :today
                 or (a.date = :today and a.startTime > :now)
              )
            """)
    List<ActivityEntity> findAvailableForUser(
            @Param("userId") UUID userId,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now);

    /**
     * Activités auxquelles l'utilisateur est inscrit en tant que participant,
     * hors celles qu'il organise lui-même (l'organisateur est aussi inscrit mais ne doit pas apparaître ici).
     */
    @Query("""
            select distinct a from ActivityEntity a
            join fetch a.type
            where a.isDeleted = false
              and a.organizer.id <> :userId
              and a.id in (select s.activityId from SubscriptionEntity s where s.userId = :userId and s.unsubscribedAt is null)
            order by a.date asc, a.startTime asc
            """)
    List<ActivityEntity> findSubscribedAsNonOrganizer(@Param("userId") UUID userId);

    @Query("""
            select case when count(a) > 0 then true else false end
            from ActivityEntity a
            where a.organizer.id = :organizerId
              and a.isDeleted = false
              and a.date = :date
              and a.startTime < :endTime
              and a.endTime > :startTime
            """)
    boolean existsOverlappingForOrganizer(
            @Param("organizerId") UUID organizerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
            select case when count(a) > 0 then true else false end
            from ActivityEntity a
            where a.organizer.id in :userIds
              and a.isDeleted = false
              and a.id <> :excludeActivityId
              and a.date = :date
              and a.startTime < :endTime
              and a.endTime > :startTime
            """)
    boolean existsOverlappingForUsersAsOrganizer(
            @Param("userIds") List<UUID> userIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeActivityId") Long excludeActivityId);

    @Query("""
            select case when count(a) > 0 then true else false end
            from ActivityEntity a
            where a.type.id = :typeId
              and a.isDeleted = false
            """)
    boolean existsNonDeletedByActivityTypeId(@Param("typeId") Long typeId);
}
