package com.example.api.activityType.infrastructure;

import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ActivityTypeRepositoryAdapter implements ActivityTypeRepositoryPort {
    private final ActivityTypeJpaRepository activityTypeJpaRepository;

    public ActivityTypeRepositoryAdapter(ActivityTypeJpaRepository activityTypeJpaRepository) {
        this.activityTypeJpaRepository = activityTypeJpaRepository;
    }


    @Override
    public ActivityType save(ActivityType activityType) {
        return null;
    }

    @Override
    public Optional<ActivityType> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<ActivityType> findAll() {
        return List.of();
    }

    @Override
    public boolean existsByName(String name) {
        return false;
    }
}
