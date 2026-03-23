package com.example.api.activity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, Long> {

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
}
