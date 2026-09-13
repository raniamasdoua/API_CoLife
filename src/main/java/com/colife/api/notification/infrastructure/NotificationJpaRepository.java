package com.colife.api.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    Optional<NotificationEntity> findByIdAndRecipientId(Long id, UUID recipientId);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.read = true where n.recipientId = :recipientId and n.read = false")
    void markAllAsReadByRecipientId(@Param("recipientId") UUID recipientId);
}
