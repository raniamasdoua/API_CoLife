package com.example.api.activityType.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTypeJpaRepository extends JpaRepository<ActivityTypeEntity, Long> {
    boolean existsByName(String name);
}
