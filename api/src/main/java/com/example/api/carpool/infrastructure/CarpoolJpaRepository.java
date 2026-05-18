package com.example.api.carpool.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarpoolJpaRepository extends JpaRepository<CarpoolEntity, Long> {
    Optional<CarpoolEntity> findByActivityId(Long activityId);
}
