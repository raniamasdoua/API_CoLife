package com.colife.api.notification.domain;

import com.colife.api.notification.infrastructure.NotificationEntity;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .recipientId(entity.getRecipientId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .activityId(entity.getActivityId())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static NotificationEntity toEntity(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(notification.getId());
        entity.setRecipientId(notification.getRecipientId());
        entity.setType(notification.getType());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setActivityId(notification.getActivityId());
        entity.setRead(notification.isRead());
        entity.setCreatedAt(notification.getCreatedAt());
        return entity;
    }
}
