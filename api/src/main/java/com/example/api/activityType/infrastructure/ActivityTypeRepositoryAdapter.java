package com.example.api.activityType.infrastructure;

import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeMapper;
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
        ActivityTypeEntity entity = new ActivityTypeEntity();
        entity.setName(activityType.getName());
        ActivityTypeEntity saved = activityTypeJpaRepository.save(entity);
        return ActivityTypeMapper.toDomain(saved);
    }

    @Override
    public Optional<ActivityType> findById(Long id) {
        return activityTypeJpaRepository.findById(id).map(ActivityTypeMapper::toDomain);
    }

    @Override
    public List<ActivityType> findAll() {
        return activityTypeJpaRepository.findAll().stream()
                .map(ActivityTypeMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return activityTypeJpaRepository.existsByName(name);
    }
}
