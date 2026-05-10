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
        ActivityTypeEntity entity;
        if (activityType.getId() != null) {
            // Le use-case garantit l'existence sur update ; ici on évite de créer une entité par erreur.
            entity = activityTypeJpaRepository.findById(activityType.getId())
                    .orElseThrow(() -> new IllegalArgumentException("ActivityTypeEntity introuvable pour update"));
        } else {
            entity = new ActivityTypeEntity();
        }

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
        return activityTypeJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public void deleteById(Long id) {
        activityTypeJpaRepository.deleteById(id);
    }

    @Override
    public long count() {
        return activityTypeJpaRepository.count();
    }
}
