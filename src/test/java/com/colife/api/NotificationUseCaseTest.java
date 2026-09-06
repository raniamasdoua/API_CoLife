package com.colife.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.notification.application.NotificationUseCase;
import com.colife.api.notification.application.dto.NotificationResponseDto;
import com.colife.api.notification.domain.Notification;
import com.colife.api.notification.domain.NotificationRepositoryPort;
import com.colife.api.notification.domain.NotificationType;
import com.colife.api.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationUseCaseTest {

    private static final UUID RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Long NOTIFICATION_ID = 200L;

    @Mock
    private NotificationRepositoryPort notificationRepository;

    private NotificationUseCase notificationUseCase;

    @BeforeEach
    void setUp() {
        notificationUseCase = new NotificationUseCase(notificationRepository);
    }

    private Notification notification(boolean read) {
        return Notification.builder()
                .id(NOTIFICATION_ID).recipientId(RECIPIENT_ID)
                .type(NotificationType.ACTIVITY_UPDATED)
                .title("Activité modifiée").message("Le créneau a changé.")
                .activityId(100L).read(read)
                .createdAt(LocalDateTime.of(2026, Month.MARCH, 15, 10, 0))
                .build();
    }

    @Test
    void listNotifications_should_return_notifications_for_recipient() {
        when(notificationRepository.findAllByRecipientId(RECIPIENT_ID)).thenReturn(List.of(notification(false)));

        List<NotificationResponseDto> result = notificationUseCase.listNotifications(RECIPIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Activité modifiée");
        assertThat(result.get(0).read()).isFalse();
    }

    @Test
    void countUnread_should_return_repository_count() {
        when(notificationRepository.countUnreadByRecipientId(RECIPIENT_ID)).thenReturn(3L);

        assertThat(notificationUseCase.countUnread(RECIPIENT_ID)).isEqualTo(3L);
    }

    @Test
    void markAsRead_should_mark_notification_as_read_for_owner() {
        when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(notification(false)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDto result = notificationUseCase.markAsRead(NOTIFICATION_ID, RECIPIENT_ID);

        assertThat(result.read()).isTrue();
    }

    @Test
    void markAsRead_should_throw_when_notification_not_found_or_not_owned() {
        when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationUseCase.markAsRead(NOTIFICATION_ID, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_should_delegate_to_repository() {
        notificationUseCase.markAllAsRead(RECIPIENT_ID);

        verify(notificationRepository).markAllAsReadByRecipientId(RECIPIENT_ID);
    }
}
