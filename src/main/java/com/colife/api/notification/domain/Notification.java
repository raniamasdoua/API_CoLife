package com.colife.api.notification.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Notification {
    private Long id;
    private UUID recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private Long activityId;
    private boolean read;
    private LocalDateTime createdAt;
}
