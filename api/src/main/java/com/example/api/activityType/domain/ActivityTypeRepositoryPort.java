package com.example.api.activityType.domain;

import java.util.List;
import java.util.Optional;

public interface ActivityTypeRepositoryPort {
    ActivityType save(ActivityType activityType);

    Optional<ActivityType> findById(Long id);

    List<ActivityType> findAll();

    boolean existsByName(String name);

    void deleteById(Long id);

    long count();
}