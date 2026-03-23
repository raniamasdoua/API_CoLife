package com.example.api.activityType.domain;

import com.example.api.activityType.infrastructure.ActivityTypeEntity;

public final class ActivityTypeMapper {

    private ActivityTypeMapper() {
    }

    public static ActivityType toDomain(ActivityTypeEntity entity) {
        return ActivityType.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
