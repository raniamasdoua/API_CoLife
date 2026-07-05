package com.colife.api.activityType.domain;

import com.colife.api.activityType.infrastructure.ActivityTypeEntity;

public final class ActivityTypeMapper {

    private ActivityTypeMapper() {
    }

    public static ActivityType toDomain(ActivityTypeEntity entity) {
        return ActivityType.builder()
                .id(entity.getId())
                .name(entity.getName())
                .deleted(entity.isDeleted())
                .build();
    }
}
