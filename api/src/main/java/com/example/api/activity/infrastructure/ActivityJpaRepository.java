package com.example.api.activity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, Long> {

    @Query("""
            select a from ActivityEntity a
            join fetch a.type
            where a.organizer.id = :organizerId
              and a.isDeleted = false
            """)
    List<ActivityEntity> findByOrganizerIdAndNotDeleted(@Param("organizerId") Long organizerId);

    @Query("""
            select a from ActivityEntity a
            join fetch a.type
            where a.organizer.id <> :organizerId
              and a.isDeleted = false
            """)
    List<ActivityEntity> findByOrganizerIdNotAndNotDeleted(@Param("organizerId") Long organizerId);

    @Query("""
            select a from ActivityEntity a
            join fetch a.type
            where a.organizer.id <> :userId
              and a.isDeleted = false
              and (
                    a.date > :today
                 or (a.date = :today and a.startTime > :now)
              )
            """)
    List<ActivityEntity> findAvailableForUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now);

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
            @Param("organizerId") Long organizerId,
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
            @Param("userIds") List<Long> userIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeActivityId") Long excludeActivityId);
}
