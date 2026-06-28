package com.example.api.activityType.infrastructure;

import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeMapper;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import com.example.api.shared.exception.ResourceNotFoundException;
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
        ActivityTypeEntity entity;
        if (activityType.getId() != null) {
            entity = activityTypeJpaRepository.findById(activityType.getId())
                    .orElseThrow(() -> new IllegalArgumentException("ActivityTypeEntity introuvable pour update"));
        } else {
            entity = new ActivityTypeEntity();
            entity.setDeleted(false);
        }

        entity.setName(activityType.getName());
        ActivityTypeEntity saved = activityTypeJpaRepository.save(entity);
        return ActivityTypeMapper.toDomain(saved);
    }

    @Override
    public Optional<ActivityType> findActiveById(Long id) {
        return activityTypeJpaRepository.findByIdAndDeletedFalse(id).map(ActivityTypeMapper::toDomain);
    }

    @Override
    public Optional<ActivityType> findByIdIncludingDeleted(Long id) {
        return activityTypeJpaRepository.findById(id).map(ActivityTypeMapper::toDomain);
    }

    @Override
    public List<ActivityType> findAllActive() {
        return activityTypeJpaRepository.findAllByDeletedFalseOrderByNameAsc().stream()
                .map(ActivityTypeMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByNameIgnoreCase(String name) {
        return activityTypeJpaRepository.existsByNameIgnoreCaseAndDeletedFalse(name);
    }

    @Override
    public void softDeleteById(Long id) {
        ActivityTypeEntity entity = activityTypeJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité introuvable"));
        if (entity.isDeleted()) {
            return;
        }
        entity.setDeleted(true);
        activityTypeJpaRepository.save(entity);
    }

    @Override
    public long countActive() {
        return activityTypeJpaRepository.countByDeletedFalse();
    }
}
