package com.example.api.activityType.application;

import com.example.api.activityType.application.dto.ActivityTypeCountDto;
import com.example.api.activityType.application.dto.ActivityTypeRequestDto;
import com.example.api.activityType.application.dto.ActivityTypeResponseDto;
import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import com.example.api.shared.exception.ConflictException;
import com.example.api.shared.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityTypeUseCase {

    private final ActivityTypeRepositoryPort repository;

    public ActivityTypeUseCase(ActivityTypeRepositoryPort repository) {
        this.repository = repository;
    }

    public List<ActivityTypeResponseDto> findAll() {
        return repository.findAllActive().stream()
                .map(type -> new ActivityTypeResponseDto(type.getId(), type.getName()))
                .toList();
    }

    public ActivityTypeCountDto count() {
        return new ActivityTypeCountDto(repository.countActive());
    }

    public ActivityTypeResponseDto findById(Long id) {
        ActivityType type = repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité introuvable"));
        return new ActivityTypeResponseDto(type.getId(), type.getName());
    }

    @Transactional
    public ActivityTypeResponseDto create(ActivityTypeRequestDto dto) {
        String name = normalizeName(dto.name());
        if (repository.existsActiveByNameIgnoreCase(name)) {
            throw new ConflictException("Ce type d'activité existe déjà");
        }

        ActivityType created = repository.save(ActivityType.builder().name(name).build());
        return new ActivityTypeResponseDto(created.getId(), created.getName());
    }

    @Transactional
    public ActivityTypeResponseDto update(Long id, ActivityTypeRequestDto dto) {
        ActivityType existing = repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'activité introuvable"));

        String name = normalizeName(dto.name());
        if (!existing.getName().equalsIgnoreCase(name) && repository.existsActiveByNameIgnoreCase(name)) {
            throw new ConflictException("Ce type d'activité existe déjà");
        }

        ActivityType updated = repository.save(
                ActivityType.builder()
                        .id(existing.getId())
                        .name(name)
                        .build()
        );
        return new ActivityTypeResponseDto(updated.getId(), updated.getName());
    }

    @Transactional
    public void delete(Long id) {
        repository.softDeleteById(id);
    }

    private static String normalizeName(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
