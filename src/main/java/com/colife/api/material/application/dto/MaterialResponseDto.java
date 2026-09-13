package com.colife.api.material.application.dto;

import java.time.LocalDateTime;

public record MaterialResponseDto(
        Long id,
        Long activityId,
        String proposedByName,
        boolean mine,
        String description,
        int quantity,
        LocalDateTime createdAt
) {
}
