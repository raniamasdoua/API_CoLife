package com.colife.api.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);

    List<Notification> findAllByRecipientId(UUID recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, UUID recipientId);

    long countUnreadByRecipientId(UUID recipientId);

    void markAllAsReadByRecipientId(UUID recipientId);
}
