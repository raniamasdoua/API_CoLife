package com.colife.api.notification.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colife.api.notification.application.dto.NotificationResponseDto;
import com.colife.api.notification.domain.Notification;
import com.colife.api.notification.domain.NotificationRepositoryPort;
import com.colife.api.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationUseCase {

    private final NotificationRepositoryPort notificationRepository;

    public NotificationUseCase(NotificationRepositoryPort notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> listNotifications(UUID recipientId) {
        return notificationRepository.findAllByRecipientId(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID recipientId) {
        return notificationRepository.countUnreadByRecipientId(recipientId);
    }

    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));

        Notification updated = Notification.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .activityId(notification.getActivityId())
                .read(true)
                .createdAt(notification.getCreatedAt())
                .build();

        return toResponse(notificationRepository.save(updated));
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepository.markAllAsReadByRecipientId(recipientId);
    }

    private NotificationResponseDto toResponse(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActivityId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
