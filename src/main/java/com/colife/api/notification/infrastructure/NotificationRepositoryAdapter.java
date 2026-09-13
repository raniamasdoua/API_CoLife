package com.colife.api.notification.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.colife.api.notification.domain.Notification;
import com.colife.api.notification.domain.NotificationMapper;
import com.colife.api.notification.domain.NotificationRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository notificationJpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationMapper.toEntity(notification);
        return NotificationMapper.toDomain(notificationJpaRepository.save(entity));
    }

    @Override
    public List<Notification> findAllByRecipientId(UUID recipientId) {
        return notificationJpaRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Notification> findByIdAndRecipientId(Long id, UUID recipientId) {
        return notificationJpaRepository.findByIdAndRecipientId(id, recipientId).map(NotificationMapper::toDomain);
    }

    @Override
    public long countUnreadByRecipientId(UUID recipientId) {
        return notificationJpaRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Override
    @Transactional
    public void markAllAsReadByRecipientId(UUID recipientId) {
        notificationJpaRepository.markAllAsReadByRecipientId(recipientId);
    }
}
