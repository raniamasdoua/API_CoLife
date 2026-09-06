package com.colife.api.notification.application.dto;

import com.colife.api.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long activityId,
        boolean read,
        LocalDateTime createdAt
) {}
