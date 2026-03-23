package com.example.api;

import com.example.api.activity.application.ActivityUseCase;
import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.activity.domain.Location;
import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import com.example.api.shared.exception.BusinessException;
import com.example.api.shared.exception.ConflictException;
import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-22T12:00:00Z"),
            ZoneId.of("Europe/Paris"));

    @Mock
    private ActivityRepositoryPort activityRepository;
    @Mock
    private ActivityTypeRepositoryPort activityTypeRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private SubscriptionRepositoryPort subscriptionRepository;

    private ActivityUseCase activityUseCase;

    @BeforeEach
    void setUp() {
        activityUseCase = new ActivityUseCase(
                activityRepository,
                activityTypeRepository,
                userRepository,
                subscriptionRepository,
                FIXED_CLOCK);
    }

    @Test
    void should_create_activity_register_organizer_as_participant() {
        Long organizerId = 5L;
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "Réunion",
                "Sprint planning",
                2L,
                LocalDate.of(2026, 3, 25),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                8,
                new LocationDto("10 rue A", null, "75001", "Paris"));

        when(userRepository.findById(organizerId)).thenReturn(Optional.of(mock(User.class)));
        ActivityType type = ActivityType.builder().id(2L).name("Réunion").build();
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(activityRepository.existsOverlappingForOrganizer(eq(organizerId), any(), any(), any()))
                .thenReturn(false);

        Activity saved = Activity.builder()
                .id(100L)
                .title(dto.title())
                .description(dto.description())
                .capacity(dto.capacity())
                .location(Location.builder()
                        .street("10 rue A")
                        .complement(null)
                        .postalCode("75001")
                        .city("Paris")
                        .build())
                .typeId(2L)
                .organizerId(organizerId)
                .date(dto.date())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .deleted(false)
                .build();
        when(activityRepository.save(any(Activity.class))).thenReturn(saved);

        ActivityResponseDto response = activityUseCase.create(organizerId, dto);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Réunion");
        assertThat(response.activityType().id()).isEqualTo(2L);
        assertThat(response.activityType().name()).isEqualTo("Réunion");
        verify(subscriptionRepository).registerParticipant(100L, organizerId);
    }

    @Test
    void should_throw_when_user_not_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.create(1L, minimalDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur");
    }

    @Test
    void should_throw_when_activity_type_not_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findById(99L)).thenReturn(Optional.empty());

        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                99L,
                LocalDate.of(2026, 4, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                new LocationDto("r", null, "c", "city"));

        assertThatThrownBy(() -> activityUseCase.create(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Type d'activité");
    }

    @Test
    void should_throw_when_date_in_past() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));

        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                2L,
                LocalDate.of(2026, 3, 20),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                new LocationDto("r", null, "c", "city"));

        assertThatThrownBy(() -> activityUseCase.create(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passé");
    }

    @Test
    void should_throw_when_end_time_not_after_start() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));

        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                2L,
                LocalDate.of(2026, 3, 30),
                LocalTime.of(10, 0),
                LocalTime.of(10, 0),
                5,
                new LocationDto("r", null, "c", "city"));

        assertThatThrownBy(() -> activityUseCase.create(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("heure de fin");
    }

    @Test
    void should_throw_when_time_overlap() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        when(activityRepository.existsOverlappingForOrganizer(eq(1L), any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.create(1L, minimalDto()))
                .isInstanceOf(ConflictException.class);
    }

    private CreateActivityRequestDto minimalDto() {
        return new CreateActivityRequestDto(
                "T",
                null,
                2L,
                LocalDate.of(2026, 3, 30),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                new LocationDto("r", null, "c", "city"));
    }
}
