package com.example.api;

import com.example.api.activity.application.ActivityUseCase;
import com.example.api.activity.application.dto.ActivityResponseDto;
import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
import com.example.api.activity.application.dto.UpdateActivityRequestDto;
import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.activity.domain.Location;
import com.example.api.activityType.domain.ActivityType;
import com.example.api.activityType.domain.ActivityTypeRepositoryPort;
import com.example.api.shared.exception.BusinessException;
import com.example.api.shared.exception.ConflictException;
import com.example.api.shared.exception.ForbiddenException;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    // ═══════════════════════════════════════════════════════════════════════
    // Tests : update()
    // Horloge fixée à 2026-03-22T12:00:00Z → Paris 13:00 (UTC+1)
    // today = 2026-03-22 | now = 13:00
    // ═══════════════════════════════════════════════════════════════════════

    private static final Long ACTIVITY_ID = 100L;
    private static final Long ORGANIZER_ID = 5L;

    private Activity futureActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Titre").description(null).capacity(10)
                .location(Location.builder().street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, 3, 30))   // futur par rapport à l'horloge
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).build();
    }

    private Activity pastActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Titre").description(null).capacity(10)
                .location(Location.builder().street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, 3, 20))   // passé par rapport à l'horloge
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).build();
    }

    private UpdateActivityRequestDto validUpdateDto() {
        return new UpdateActivityRequestDto(
                "Nouveau titre", "Nouvelle description", 2L,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                8,
                new LocationDto("10 rue B", null, "75002", "Paris"));
    }

    private Activity savedActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Nouveau titre").description("Nouvelle description").capacity(8)
                .location(Location.builder().street("10 rue B").postalCode("75002").city("Paris").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, 4, 10))
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .deleted(false).build();
    }

    @Test
    void should_throw_not_found_when_activity_does_not_exist() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Activité");
    }

    @Test
    void should_throw_forbidden_when_caller_is_not_organizer_and_not_admin() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        Long otherUserId = 99L;

        assertThatThrownBy(() -> activityUseCase.update(otherUserId, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_allow_update_when_caller_is_admin_even_if_not_organizer() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(activityRepository.update(any())).thenReturn(savedActivity());
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        assertThatCode(() -> activityUseCase.update(99L, true, ACTIVITY_ID, validUpdateDto()))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_activity_is_already_past() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passée");
    }

    @Test
    void should_throw_not_found_when_activity_type_does_not_exist_for_update() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Type d'activité");
    }

    @Test
    void should_throw_when_new_capacity_is_below_current_participant_count() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        UpdateActivityRequestDto dtoWithLowCapacity = new UpdateActivityRequestDto(
                "Titre", null, 2L,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                2,
                new LocationDto("r", null, "p", "c"));

        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(5);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, dtoWithLowCapacity))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("participants actuels");
    }

    @Test
    void should_throw_conflict_when_organizer_has_overlapping_activity_as_organizer() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(activityRepository.existsOverlappingForUsersAsOrganizer(anyList(), any(), any(), any(), anyLong()))
                .thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("organisateur");
    }

    @Test
    void should_throw_conflict_when_organizer_is_subscribed_to_overlapping_activity() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(subscriptionRepository.existsConflictingActivityForSubscribedUsers(anyList(), any(), any(), any(), anyLong()))
                .thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("organisateur");
    }

    @Test
    void should_throw_conflict_when_participant_has_overlapping_activity_as_organizer() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        Long participantId = 10L;
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(2);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(ORGANIZER_ID, participantId));
        // 1er appel (organisateur) → false ; 2e appel (participant) → true
        when(activityRepository.existsOverlappingForUsersAsOrganizer(anyList(), any(), any(), any(), anyLong()))
                .thenReturn(false, true);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("participants");
    }

    @Test
    void should_throw_conflict_when_participant_is_subscribed_to_overlapping_activity() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        Long participantId = 10L;
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(2);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(ORGANIZER_ID, participantId));
        // 1er appel (organisateur) → false ; 2e appel (participant) → true
        when(subscriptionRepository.existsConflictingActivityForSubscribedUsers(anyList(), any(), any(), any(), anyLong()))
                .thenReturn(false, true);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("participants");
    }

    @Test
    void should_update_activity_and_return_correct_response() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        Activity saved = savedActivity();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(activityRepository.update(any(Activity.class))).thenReturn(saved);
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        ActivityResponseDto result = activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto());

        assertThat(result.id()).isEqualTo(ACTIVITY_ID);
        assertThat(result.title()).isEqualTo("Nouveau titre");
        assertThat(result.capacity()).isEqualTo(8);
        assertThat(result.activityType().name()).isEqualTo("Sport");
        assertThat(result.participantCount()).isEqualTo(1);
        verify(activityRepository).update(any(Activity.class));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tests : delete()
    // ═══════════════════════════════════════════════════════════════════════

    private Activity deletedFutureActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Titre").description(null).capacity(10)
                .location(Location.builder().street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, 3, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(true)
                .build();
    }

    @Test
    void should_delete_when_organizer_calls_delete() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        activityUseCase.delete(ORGANIZER_ID, false, ACTIVITY_ID);

        verify(subscriptionRepository).deleteAllByActivityId(ACTIVITY_ID);
        verify(activityRepository).softDelete(ACTIVITY_ID);
    }

    @Test
    void should_delete_when_admin_calls_delete() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        activityUseCase.delete(99L, true, ACTIVITY_ID);

        verify(subscriptionRepository).deleteAllByActivityId(ACTIVITY_ID);
        verify(activityRepository).softDelete(ACTIVITY_ID);
    }

    @Test
    void should_throw_not_found_when_activity_missing_on_delete() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.delete(ORGANIZER_ID, false, ACTIVITY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriptionRepository, never()).deleteAllByActivityId(anyLong());
        verify(activityRepository, never()).softDelete(anyLong());
    }

    @Test
    void should_throw_not_found_when_activity_already_soft_deleted() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(deletedFutureActivity()));

        assertThatThrownBy(() -> activityUseCase.delete(ORGANIZER_ID, false, ACTIVITY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriptionRepository, never()).deleteAllByActivityId(anyLong());
    }

    @Test
    void should_throw_forbidden_when_not_organizer_and_not_admin_on_delete() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        assertThatThrownBy(() -> activityUseCase.delete(99L, false, ACTIVITY_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(subscriptionRepository, never()).deleteAllByActivityId(anyLong());
        verify(activityRepository, never()).softDelete(anyLong());
    }

    @Test
    void should_throw_when_activity_past_on_delete() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> activityUseCase.delete(ORGANIZER_ID, false, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passée");
        verify(subscriptionRepository, never()).deleteAllByActivityId(anyLong());
    }
}
