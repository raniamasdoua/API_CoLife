package com.example.api.activityType.application;

import com.example.api.activityType.application.dto.ActivityTypeResponseDto;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityTypeUseCase {

    private final ActivityTypeRepositoryPort repository;

    public ActivityTypeUseCase(ActivityTypeRepositoryPort repository) {
        this.repository = repository;
    }

    public List<ActivityTypeResponseDto> findAll() {
        return repository.findAll().stream()
                .map(type -> new ActivityTypeResponseDto(type.getId(), type.getName()))
                .toList();
    }
}
