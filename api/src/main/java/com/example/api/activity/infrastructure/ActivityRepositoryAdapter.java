package com.example.api.activity.infrastructure;

import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {
    private final ActivityJpaRepository activityJpaRepository;
    public ActivityRepositoryAdapter(ActivityJpaRepository activityJpaRepository) {
        this.activityJpaRepository = activityJpaRepository;
    }

    @Override
    public Optional<Activity> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Activity save(Activity activity) {
        return null;
    }

    @Override
    public List<Activity> findAll() {
        return List.of();
    }
}
