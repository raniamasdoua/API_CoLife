package com.colife.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.activity.application.ActivityUseCase;
import com.colife.api.activity.application.dto.ActivityResponseDto;
import com.colife.api.activity.application.dto.CreateActivityRequestDto;
import com.colife.api.activity.application.dto.LocationDto;
import com.colife.api.activity.application.dto.UpdateActivityRequestDto;
import com.colife.api.activity.domain.Activity;
import com.colife.api.activity.domain.ActivityRepositoryPort;
import com.colife.api.activity.domain.Location;
import com.colife.api.activity.domain.LocationType;
import com.colife.api.activityType.domain.ActivityType;
import com.colife.api.activityType.domain.ActivityTypeRepositoryPort;
import com.colife.api.carpool.application.dto.CarpoolRequestDto;
import com.colife.api.carpool.domain.Carpool;
import com.colife.api.carpool.domain.CarpoolPassengerRepositoryPort;
import com.colife.api.carpool.domain.CarpoolRepositoryPort;
import com.colife.api.material.domain.MaterialRepositoryPort;
import com.colife.api.shared.exception.BusinessException;
import com.colife.api.shared.exception.ConflictException;
import com.colife.api.shared.exception.ForbiddenException;
import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.shared.notification.NotificationPort;
import com.colife.api.subscription.domain.SubscriptionRepositoryPort;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-22T12:00:00Z"),
            ZoneId.of("Europe/Paris"));

    private static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID PARTICIPANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Long ACTIVITY_ID = 100L;

    @Mock
    private ActivityRepositoryPort activityRepository;
    @Mock
    private ActivityTypeRepositoryPort activityTypeRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private SubscriptionRepositoryPort subscriptionRepository;
    @Mock
    private CarpoolRepositoryPort carpoolRepository;
    @Mock
    private CarpoolPassengerRepositoryPort carpoolPassengerRepository;
    @Mock
    private MaterialRepositoryPort materialRepository;
    @Mock
    private NotificationPort notificationPort;

    private ActivityUseCase activityUseCase;

    @BeforeEach
    void setUp() {
        activityUseCase = new ActivityUseCase(
                activityRepository,
                activityTypeRepository,
                userRepository,
                subscriptionRepository,
                carpoolRepository,
                carpoolPassengerRepository,
                materialRepository,
                notificationPort,
                FIXED_CLOCK);
    }

    /** Localisation hors site valide (room null, adresse renseignÃ©e). */
    private static LocationDto offSite(String street, String postalCode, String city) {
        return new LocationDto(null, street, null, postalCode, city);
    }

    @Test
    void should_create_activity_register_organizer_as_participant() {
        UUID organizerId = ORGANIZER_ID;
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "RÃ©union",
                "Sprint planning",
                2L,
                LocalDate.of(2026, 3, 25),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                8,
                offSite("10 rue A", "75001", "Paris"),
                LocationType.OFF_SITE,
                null,
                null);

        when(userRepository.findById(organizerId)).thenReturn(Optional.of(mock(User.class)));
        ActivityType type = ActivityType.builder().id(2L).name("RÃ©union").build();
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(activityRepository.existsOverlappingForOrganizer(eq(organizerId), any(), any(), any()))
                .thenReturn(false);

        Activity saved = Activity.builder()
                .id(100L)
                .title(dto.title())
                .description(dto.description())
                .capacity(dto.capacity())
                .location(Location.builder()
                        .locationType(LocationType.OFF_SITE)
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
                .locationType(LocationType.OFF_SITE)
                .build();
        when(activityRepository.save(any(Activity.class))).thenReturn(saved);

        ActivityResponseDto response = activityUseCase.create(organizerId, dto);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("RÃ©union");
        assertThat(response.activityType().id()).isEqualTo(2L);
        assertThat(response.activityType().name()).isEqualTo("RÃ©union");
        verify(subscriptionRepository).registerParticipant(100L, organizerId);
    }

    @Test
    void should_throw_when_user_not_found() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, minimalDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur");
    }

    @Test
    void should_throw_when_activity_type_not_found() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(99L)).thenReturn(Optional.empty());

        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                99L,
                LocalDate.of(2026, Month.APRIL, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                offSite("r", "c", "city"),
                LocationType.OFF_SITE,
                null,
                null);

        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_when_date_in_past() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));

        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                2L,
                LocalDate.of(2026, 3, 20),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                offSite("r", "c", "city"),
                LocationType.OFF_SITE,
                null,
                null);

        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throw_when_end_time_not_after_start() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));

        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                2L,
                LocalDate.of(2026, 3, 30),
                LocalTime.of(10, 0),
                LocalTime.of(10, 0),
                5,
                offSite("r", "c", "city"),
                LocationType.OFF_SITE,
                null,
                null);

        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("heure de fin");
    }

    @Test
    void should_throw_when_time_overlap() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        when(activityRepository.existsOverlappingForOrganizer(eq(ORGANIZER_ID), any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, minimalDto()))
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
                offSite("r", "c", "city"),
                LocationType.OFF_SITE,
                null,
                null);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Tests : update()
    // Horloge fixÃ©e Ã  2026-03-22T12:00:00Z â†’ Paris 13:00 (UTC+1)
    // today = 2026-03-22 | now = 13:00
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private Activity futureActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Titre").description(null).capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))   // futur par rapport Ã  l'horloge
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    private Activity pastActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Titre").description(null).capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 20))   // passÃ© par rapport Ã  l'horloge
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    private UpdateActivityRequestDto validUpdateDto() {
        return new UpdateActivityRequestDto(
                "Nouveau titre", "Nouvelle description", 2L,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                8,
                offSite("10 rue B", "75002", "Paris"),
                LocationType.OFF_SITE);
    }

    private Activity savedActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Nouveau titre").description("Nouvelle description").capacity(8)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("10 rue B").postalCode("75002").city("Paris").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, 4, 10))
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    @Test
    void should_throw_not_found_when_activity_does_not_exist() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_forbidden_when_caller_is_not_organizer_and_not_admin() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        assertThatThrownBy(() -> activityUseCase.update(OTHER_USER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_allow_update_when_caller_is_admin_even_if_not_organizer() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(activityRepository.update(any())).thenReturn(savedActivity());
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        assertThatCode(() -> activityUseCase.update(OTHER_USER_ID, true, ACTIVITY_ID, validUpdateDto()))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_activity_is_already_past() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throw_not_found_when_activity_type_does_not_exist_for_update() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_when_new_capacity_is_below_current_participant_count() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        UpdateActivityRequestDto dtoWithLowCapacity = new UpdateActivityRequestDto(
                "Titre", null, 2L,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                2,
                offSite("r", "p", "c"),
                LocationType.OFF_SITE);

        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(5);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, dtoWithLowCapacity))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("participants actuels");
    }

    @Test
    void should_throw_conflict_when_organizer_has_overlapping_activity_as_organizer() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
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
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
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
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(2);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(ORGANIZER_ID, PARTICIPANT_ID));
        // 1er appel (organisateur) â†’ false ; 2e appel (participant) â†’ true
        when(activityRepository.existsOverlappingForUsersAsOrganizer(anyList(), any(), any(), any(), anyLong()))
                .thenReturn(false, true);

        assertThatThrownBy(() -> activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("participants");
    }

    @Test
    void should_throw_conflict_when_participant_is_subscribed_to_overlapping_activity() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(2);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(ORGANIZER_ID, PARTICIPANT_ID));
        // 1er appel (organisateur) â†’ false ; 2e appel (participant) â†’ true
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
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
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

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Tests : delete()
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private Activity deletedFutureActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Titre").description(null).capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, 3, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(true)
                .locationType(LocationType.OFF_SITE)
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

        activityUseCase.delete(OTHER_USER_ID, true, ACTIVITY_ID);

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

        assertThatThrownBy(() -> activityUseCase.delete(OTHER_USER_ID, false, ACTIVITY_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(subscriptionRepository, never()).deleteAllByActivityId(anyLong());
        verify(activityRepository, never()).softDelete(anyLong());
    }

    @Test
    void should_throw_when_activity_past_on_delete() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> activityUseCase.delete(ORGANIZER_ID, false, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
        verify(subscriptionRepository, never()).deleteAllByActivityId(anyLong());
    }

    @Test
    void should_unsubscribe_participant_and_return_updated_count() {
        UUID participantId = PARTICIPANT_ID;
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, participantId)).thenReturn(true);
        doNothing().when(subscriptionRepository).unsubscribeParticipant(ACTIVITY_ID, participantId);
        when(activityTypeRepository.findByIdIncludingDeleted(2L)).thenReturn(Optional.of(type));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(3);

        ActivityResponseDto result = activityUseCase.unsubscribe(participantId, ACTIVITY_ID);

        assertThat(result.participantCount()).isEqualTo(3);
        verify(subscriptionRepository).unsubscribeParticipant(ACTIVITY_ID, participantId);
    }

    @Test
    void should_reject_unsubscribe_when_organizer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        assertThatThrownBy(() -> activityUseCase.unsubscribe(ORGANIZER_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("organisateur");

        verify(subscriptionRepository, never()).unsubscribeParticipant(anyLong(), any());
    }

    @Test
    void should_reject_unsubscribe_when_not_subscribed() {
        UUID participantId = PARTICIPANT_ID;
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, participantId)).thenReturn(false);

        assertThatThrownBy(() -> activityUseCase.unsubscribe(participantId, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);

        verify(subscriptionRepository, never()).unsubscribeParticipant(anyLong(), any());
    }
    // ─── Helpers supplémentaires ─────────────────────────────────────────────

    private static final Long TYPE_ID = 2L;

    private static ActivityType sampleType() {
        return ActivityType.builder().id(TYPE_ID).name("Réunion").build();
    }

    private static User organizer() {
        return User.builder().id(ORGANIZER_ID).firstName("Jean").lastName("Dupont")
                .email("jean@test.com").build();
    }

    private static User participant() {
        return User.builder().id(PARTICIPANT_ID).firstName("Marie").lastName("Martin")
                .email("marie@test.com").build();
    }

    /** Prépare les mocks communs à toResponseList (appelé par getMyActivities, etc.). */
    private void stubToResponseList() {
        when(activityTypeRepository.findByIdIncludingDeleted(TYPE_ID)).thenReturn(Optional.of(sampleType()));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(organizer()));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(carpoolRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());
    }

    // ─── Tests : subscribe ───────────────────────────────────────────────────

    @Test
    void should_subscribe_successfully() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(false);
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(3);
        when(activityRepository.existsOverlappingForUsersAsOrganizer(anyList(), any(), any(), any(), anyLong())).thenReturn(false);
        when(subscriptionRepository.existsConflictingActivityForSubscribedUsers(anyList(), any(), any(), any(), anyLong())).thenReturn(false);
        when(activityTypeRepository.findByIdIncludingDeleted(TYPE_ID)).thenReturn(Optional.of(sampleType()));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(organizer()));
        when(carpoolRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());

        ActivityResponseDto result = activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID);

        assertThat(result).isNotNull();
        verify(subscriptionRepository).registerParticipant(ACTIVITY_ID, PARTICIPANT_ID);
    }

    @Test
    void should_throw_when_user_not_found_on_subscribe() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriptionRepository, never()).registerParticipant(anyLong(), any());
    }

    @Test
    void should_throw_when_activity_not_found_on_subscribe() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_when_activity_deleted_on_subscribe() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(deletedFutureActivity()));

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_when_organizer_tries_to_subscribe() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(organizer()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        assertThatThrownBy(() -> activityUseCase.subscribe(ORGANIZER_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propre activité");
    }

    @Test
    void should_throw_when_activity_past_on_subscribe() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throw_when_already_subscribed() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà inscrit");
    }

    @Test
    void should_throw_when_activity_full_on_subscribe() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        Activity full = Activity.builder()
                .id(ACTIVITY_ID).title("Titre").capacity(2)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(TYPE_ID).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(full));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(false);
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(2);

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Capacité maximale");
    }

    @Test
    void should_throw_when_time_conflict_on_subscribe() {
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(false);
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(3);
        when(activityRepository.existsOverlappingForUsersAsOrganizer(anyList(), any(), any(), any(), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.subscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("conflit");
    }

    // ─── Tests : getParticipants ─────────────────────────────────────────────

    @Test
    void getParticipants_should_return_participant_list() {
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(PARTICIPANT_ID));
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant()));

        var result = activityUseCase.getParticipants(ACTIVITY_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(PARTICIPANT_ID);
    }

    @Test
    void getParticipants_should_return_empty_when_no_subscriptions() {
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of());

        var result = activityUseCase.getParticipants(ACTIVITY_ID);

        assertThat(result).isEmpty();
    }

    // ─── Tests : getAllActivities ─────────────────────────────────────────────

    @Test
    void getAllActivities_should_exclude_deleted_by_default() {
        Activity deleted = Activity.builder()
                .id(200L).title("Supprimée").capacity(5)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(TYPE_ID).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.APRIL, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .deleted(true).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findAll()).thenReturn(List.of(futureActivity(), deleted));
        stubToResponseList();

        var result = activityUseCase.getAllActivities(false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(ACTIVITY_ID);
    }

    @Test
    void getAllActivities_should_include_deleted_when_flag_true() {
        Activity deleted = Activity.builder()
                .id(200L).title("Supprimée").capacity(5)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(TYPE_ID).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.APRIL, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .deleted(true).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findAll()).thenReturn(List.of(futureActivity(), deleted));
        stubToResponseList();

        var result = activityUseCase.getAllActivities(true);

        assertThat(result).hasSize(2);
    }

    // ─── Tests : getMyActivities / getRegisteredActivities / getAvailableActivities ──

    @Test
    void getMyActivities_should_return_organized_activities() {
        when(activityRepository.findByOrganizerId(ORGANIZER_ID)).thenReturn(List.of(futureActivity()));
        stubToResponseList();

        var result = activityUseCase.getMyActivities(ORGANIZER_ID);

        assertThat(result).hasSize(1);
        verify(activityRepository).findByOrganizerId(ORGANIZER_ID);
    }

    @Test
    void getRegisteredActivities_should_return_subscribed_activities() {
        when(activityRepository.findSubscribedAsNonOrganizer(PARTICIPANT_ID)).thenReturn(List.of(futureActivity()));
        stubToResponseList();

        var result = activityUseCase.getRegisteredActivities(PARTICIPANT_ID);

        assertThat(result).hasSize(1);
        verify(activityRepository).findSubscribedAsNonOrganizer(PARTICIPANT_ID);
    }

    @Test
    void getUserActivities_should_return_organized_and_registered_activities() {
        when(activityRepository.findByOrganizerId(PARTICIPANT_ID)).thenReturn(List.of(futureActivity()));
        when(activityRepository.findSubscribedAsNonOrganizer(PARTICIPANT_ID)).thenReturn(List.of(futureActivity()));
        stubToResponseList();

        var result = activityUseCase.getUserActivities(PARTICIPANT_ID);

        assertThat(result.organized()).hasSize(1);
        assertThat(result.registered()).hasSize(1);
    }

    @Test
    void getAvailableActivities_should_exclude_own_activities() {
        Activity ownActivity = Activity.builder()
                .id(300L).title("La mienne").capacity(5)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(TYPE_ID).organizerId(PARTICIPANT_ID)
                .date(LocalDate.of(2026, Month.APRIL, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findAvailableForUser(eq(PARTICIPANT_ID), any(), any()))
                .thenReturn(List.of(futureActivity(), ownActivity));
        stubToResponseList();

        var result = activityUseCase.getAvailableActivities(PARTICIPANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(ACTIVITY_ID);
    }

    // ─── Tests : unsubscribe ──────────────────────────────────────────────────

    @Test
    void should_unsubscribe_successfully() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(true);
        when(activityTypeRepository.findByIdIncludingDeleted(TYPE_ID)).thenReturn(Optional.of(sampleType()));

        ActivityResponseDto result = activityUseCase.unsubscribe(PARTICIPANT_ID, ACTIVITY_ID);

        assertThat(result).isNotNull();
        verify(subscriptionRepository).unsubscribeParticipant(ACTIVITY_ID, PARTICIPANT_ID);
    }

    @Test
    void should_throw_when_activity_not_found_on_unsubscribe() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityUseCase.unsubscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_when_organizer_tries_to_unsubscribe() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));

        assertThatThrownBy(() -> activityUseCase.unsubscribe(ORGANIZER_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throw_when_not_subscribed_on_unsubscribe() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> activityUseCase.unsubscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throw_when_activity_past_on_unsubscribe() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(pastActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> activityUseCase.unsubscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_cancel_driver_carpool_when_driver_unsubscribes() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(true);
        Carpool driverCarpool = Carpool.builder().id(5L).activityId(ACTIVITY_ID).driverId(PARTICIPANT_ID)
                .departureTime(LocalTime.of(8, 0)).maxPassengers(3).build();
        when(carpoolRepository.findActiveByDriverIdAndActivityId(PARTICIPANT_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(driverCarpool));
        when(activityTypeRepository.findByIdIncludingDeleted(TYPE_ID)).thenReturn(Optional.of(sampleType()));

        ActivityResponseDto result = activityUseCase.unsubscribe(PARTICIPANT_ID, ACTIVITY_ID);

        assertThat(result).isNotNull();
        verify(carpoolPassengerRepository).removeAllByCarpoolId(5L);
        verify(carpoolRepository).cancelByDriverIdAndActivityId(PARTICIPANT_ID, ACTIVITY_ID);
    }

    @Test
    void should_throw_business_exception_when_unsubscribe_participant_fails() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, PARTICIPANT_ID)).thenReturn(true);
        doThrow(new IllegalStateException("DB error"))
                .when(subscriptionRepository).unsubscribeParticipant(ACTIVITY_ID, PARTICIPANT_ID);

        assertThatThrownBy(() -> activityUseCase.unsubscribe(PARTICIPANT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finaliser");
    }

    // ─── Tests : create() – validateLocation ──────────────────────────────────

    @Test
    void should_throw_when_on_site_activity_has_no_room() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        LocationDto noRoom = new LocationDto(null, null, null, null, null);
        CreateActivityRequestDto dto = new CreateActivityRequestDto("T", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                noRoom, LocationType.ON_SITE, null, null);
        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("salle");
    }

    @Test
    void should_throw_when_off_site_activity_has_no_street() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        LocationDto noStreet = new LocationDto(null, null, null, "75001", "Paris");
        CreateActivityRequestDto dto = new CreateActivityRequestDto("T", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                noStreet, LocationType.OFF_SITE, null, null);
        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rue");
    }

    @Test
    void should_throw_when_off_site_activity_has_no_postal_code() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        LocationDto noPostal = new LocationDto(null, "1 rue A", null, null, "Paris");
        CreateActivityRequestDto dto = new CreateActivityRequestDto("T", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                noPostal, LocationType.OFF_SITE, null, null);
        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("code postal");
    }

    @Test
    void should_throw_when_off_site_activity_has_no_city() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        LocationDto noCity = new LocationDto(null, "1 rue A", null, "75001", null);
        CreateActivityRequestDto dto = new CreateActivityRequestDto("T", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                noCity, LocationType.OFF_SITE, null, null);
        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ville");
    }

    @Test
    void should_throw_when_carpool_requested_for_on_site_activity() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(
                ActivityType.builder().id(2L).name("X").build()));
        LocationDto onSite = new LocationDto("Salle A", null, null, null, null);
        CarpoolRequestDto carpoolDto = new CarpoolRequestDto(LocalTime.of(8, 0), 3);
        CreateActivityRequestDto dto = new CreateActivityRequestDto("T", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                onSite, LocationType.ON_SITE, carpoolDto, null);
        assertThatThrownBy(() -> activityUseCase.create(ORGANIZER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("covoiturage");
    }

    @Test
    void should_create_activity_with_carpool_successfully() {
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        ActivityType type = ActivityType.builder().id(2L).name("X").build();
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        Activity saved = Activity.builder()
                .id(ACTIVITY_ID).title("T").capacity(5)
                .location(Location.builder().locationType(LocationType.OFF_SITE)
                        .street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.save(any(Activity.class))).thenReturn(saved);
        Carpool savedCarpool = Carpool.builder()
                .id(5L).activityId(ACTIVITY_ID).driverId(ORGANIZER_ID)
                .departureTime(LocalTime.of(8, 0)).maxPassengers(3).build();
        when(carpoolRepository.save(any(Carpool.class))).thenReturn(savedCarpool);
        CarpoolRequestDto carpoolDto = new CarpoolRequestDto(LocalTime.of(8, 0), 3);
        CreateActivityRequestDto dto = new CreateActivityRequestDto("T", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                offSite("r", "p", "c"), LocationType.OFF_SITE, carpoolDto, null);

        ActivityResponseDto result = activityUseCase.create(ORGANIZER_ID, dto);

        assertThat(result.carpool()).isNotNull();
        assertThat(result.carpool().driverId()).isEqualTo(ORGANIZER_ID);
        verify(carpoolRepository).save(any(Carpool.class));
    }

    // ─── Tests : update() – switchingToOnSite ─────────────────────────────────

    @Test
    void should_cancel_carpools_when_activity_switches_to_on_site() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(futureActivity()));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        Carpool activeCarpool = Carpool.builder().id(5L).activityId(ACTIVITY_ID).driverId(ORGANIZER_ID)
                .departureTime(LocalTime.of(8, 0)).maxPassengers(3).build();
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of(activeCarpool));
        when(activityRepository.update(any(Activity.class))).thenReturn(savedActivity());
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));
        UpdateActivityRequestDto onSiteDto = new UpdateActivityRequestDto(
                "Titre", null, 2L,
                LocalDate.of(2026, Month.APRIL, 10),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                8,
                new LocationDto("Salle A", null, null, null, null),
                LocationType.ON_SITE);

        activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, onSiteDto);

        verify(carpoolPassengerRepository).removeAllByCarpoolIds(List.of(5L));
        verify(carpoolRepository).cancelByIds(List.of(5L));
    }

    // ─── Tests : update() – covoiturage invalidé par un changement d'horaire ──

    @Test
    void should_cancel_carpool_and_notify_driver_when_new_start_time_invalidates_departure() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        Activity existing = Activity.builder()
                .id(ACTIVITY_ID).title("Sortie").description(null).capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(existing));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(activityRepository.update(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        // Départ à 9h, initialement valide (< 10h). Reproduit le cas testé manuellement :
        // organisateur avance le début de l'activité à 8h → le départ à 9h devient incohérent.
        Carpool carpool = Carpool.builder().id(7L).activityId(ACTIVITY_ID).driverId(PARTICIPANT_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3).build();
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of(carpool));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(7L)).thenReturn(List.of());

        User driver = mock(User.class);
        when(driver.getFirstName()).thenReturn("Sam");
        when(driver.getEmail()).thenReturn("sam@test.fr");
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(driver));

        UpdateActivityRequestDto dto = new UpdateActivityRequestDto(
                "Sortie", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(8, 0), LocalTime.of(11, 0),
                10,
                offSite("r", "p", "c"),
                LocationType.OFF_SITE);

        activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, dto);

        verify(carpoolPassengerRepository).removeAllByCarpoolIds(List.of(7L));
        verify(carpoolRepository).cancelByIds(List.of(7L));
        verify(notificationPort).send(eq("sam@test.fr"), any(String.class), any(String.class));
    }

    @Test
    void should_not_cancel_carpool_when_departure_still_valid_after_schedule_change() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        Activity existing = Activity.builder()
                .id(ACTIVITY_ID).title("Sortie").description(null).capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE).street("r").postalCode("p").city("c").complement(null).build())
                .typeId(2L).organizerId(ORGANIZER_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(existing));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(1);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of(ORGANIZER_ID));
        when(activityRepository.update(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        // Départ à 9h : le nouvel horaire (10h30) reste compatible, aucune annulation attendue.
        Carpool carpool = Carpool.builder().id(7L).activityId(ACTIVITY_ID).driverId(PARTICIPANT_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3).build();
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of(carpool));

        UpdateActivityRequestDto dto = new UpdateActivityRequestDto(
                "Sortie", null, 2L,
                LocalDate.of(2026, Month.MARCH, 30),
                LocalTime.of(10, 30), LocalTime.of(11, 30),
                10,
                offSite("r", "p", "c"),
                LocationType.OFF_SITE);

        activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, dto);

        verify(carpoolPassengerRepository, never()).removeAllByCarpoolIds(anyList());
        verify(carpoolRepository, never()).cancelByIds(anyList());
    }

    @Test
    void should_notify_participant_but_not_organizer_when_schedule_changes() {
        ActivityType type = ActivityType.builder().id(2L).name("Sport").build();
        Activity existing = futureActivity();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(existing));
        when(activityTypeRepository.findActiveById(2L)).thenReturn(Optional.of(type));
        when(subscriptionRepository.countParticipants(ACTIVITY_ID)).thenReturn(2);
        when(subscriptionRepository.findUserIdsByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(ORGANIZER_ID, PARTICIPANT_ID));
        when(activityRepository.update(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(mock(User.class)));

        User participant = mock(User.class);
        when(participant.getFirstName()).thenReturn("Ana");
        when(participant.getEmail()).thenReturn("ana@test.fr");
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));

        activityUseCase.update(ORGANIZER_ID, false, ACTIVITY_ID, validUpdateDto());

        // Un seul email envoyé, et uniquement au participant (l'organisateur est à l'origine du changement).
        verify(notificationPort, times(1)).send(any(String.class), any(String.class), any(String.class));
        verify(notificationPort).send(eq("ana@test.fr"), any(String.class), any(String.class));
    }
}
