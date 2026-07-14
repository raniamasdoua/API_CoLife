package com.colife.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.activity.domain.Activity;
import com.colife.api.activity.domain.ActivityRepositoryPort;
import com.colife.api.activity.domain.Location;
import com.colife.api.activity.domain.LocationType;
import com.colife.api.carpool.application.CarpoolUseCase;
import com.colife.api.carpool.application.dto.ActivityCarpoolsResponseDto;
import com.colife.api.carpool.application.dto.CarpoolDetailDto;
import com.colife.api.carpool.application.dto.CarpoolRequestDto;
import com.colife.api.carpool.domain.Carpool;
import com.colife.api.carpool.domain.CarpoolPassenger;
import com.colife.api.carpool.domain.CarpoolPassengerRepositoryPort;
import com.colife.api.carpool.domain.CarpoolRepositoryPort;
import com.colife.api.carpool.domain.CarpoolStatus;
import com.colife.api.shared.exception.BusinessException;
import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.subscription.domain.SubscriptionRepositoryPort;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarpoolUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-22T12:00:00Z"),
            ZoneId.of("Europe/Paris"));

    private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Long ACTIVITY_ID = 100L;
    private static final Long CARPOOL_ID = 200L;

    @Mock
    private CarpoolRepositoryPort carpoolRepository;
    @Mock
    private CarpoolPassengerRepositoryPort carpoolPassengerRepository;
    @Mock
    private ActivityRepositoryPort activityRepository;
    @Mock
    private SubscriptionRepositoryPort subscriptionRepository;
    @Mock
    private UserRepositoryPort userRepository;

    private CarpoolUseCase carpoolUseCase;

    @BeforeEach
    void setUp() {
        carpoolUseCase = new CarpoolUseCase(
                carpoolRepository,
                carpoolPassengerRepository,
                activityRepository,
                subscriptionRepository,
                userRepository,
                FIXED_CLOCK);
    }

    private Activity offSiteFutureActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Sortie").capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE)
                        .street("r").postalCode("p").city("c").build())
                .typeId(2L).organizerId(ORG_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    private Activity onSiteActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Réunion").capacity(10)
                .location(Location.builder().locationType(LocationType.ON_SITE).room("A1").build())
                .typeId(2L).organizerId(ORG_ID)
                .date(LocalDate.of(2026, Month.MARCH, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.ON_SITE).build();
    }

    private Carpool activeCarpool(UUID driverId) {
        return Carpool.builder()
                .id(CARPOOL_ID).activityId(ACTIVITY_ID).driverId(driverId)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3)
                .status(CarpoolStatus.ACTIVE).build();
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    void list_throws_when_activity_is_on_site() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(onSiteActivity()));

        assertThatThrownBy(() -> carpoolUseCase.listCarpools(ACTIVITY_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hors site");
    }

    @Test
    void list_returns_driver_role_for_current_driver() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(activeCarpool(USER_ID)));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(CARPOOL_ID))
                .thenReturn(List.of());

        ActivityCarpoolsResponseDto result = carpoolUseCase.listCarpools(ACTIVITY_ID, USER_ID);

        assertThat(result.userRole()).isEqualTo("DRIVER");
        assertThat(result.userCarpoolId()).isEqualTo(CARPOOL_ID);
        assertThat(result.carpools()).hasSize(1);
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void create_throws_when_activity_is_on_site() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(onSiteActivity()));

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        assertThatThrownBy(() -> carpoolUseCase.createCarpool(ACTIVITY_ID, USER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hors site");
    }

    @Test
    void create_throws_when_user_not_subscribed_and_not_organizer() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(false);

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        assertThatThrownBy(() -> carpoolUseCase.createCarpool(ACTIVITY_ID, USER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inscrit");
    }

    @Test
    void create_succeeds_for_subscribed_user() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(carpoolRepository.findActiveByDriverIdAndActivityId(USER_ID, ACTIVITY_ID)).thenReturn(Optional.empty());
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of());
        when(carpoolRepository.save(org.mockito.ArgumentMatchers.any(Carpool.class)))
                .thenReturn(activeCarpool(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(CARPOOL_ID)).thenReturn(List.of());

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        CarpoolDetailDto result = carpoolUseCase.createCarpool(ACTIVITY_ID, USER_ID, dto);

        assertThat(result.driverId()).isEqualTo(USER_ID);
        assertThat(result.status()).isEqualTo(CarpoolStatus.ACTIVE);
        assertThat(result.availableSeats()).isEqualTo(3);
        verify(carpoolRepository).save(org.mockito.ArgumentMatchers.any(Carpool.class));
    }

    // ─── join ───────────────────────────────────────────────────────────────

    @Test
    void join_throws_when_user_not_subscribed() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> carpoolUseCase.joinCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inscrit");
    }

    @Test
    void join_succeeds_when_seats_available() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(ORG_ID)));
        when(carpoolRepository.findActiveByDriverIdAndActivityId(USER_ID, ACTIVITY_ID)).thenReturn(Optional.empty());
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of(activeCarpool(ORG_ID)));
        when(carpoolPassengerRepository.findActiveByPassengerIdAndCarpoolIds(USER_ID, List.of(CARPOOL_ID)))
                .thenReturn(Optional.empty());
        when(carpoolPassengerRepository.countActive(CARPOOL_ID)).thenReturn(0);
        when(userRepository.findById(ORG_ID)).thenReturn(Optional.of(mock(User.class)));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(CARPOOL_ID)).thenReturn(List.of());

        CarpoolDetailDto result = carpoolUseCase.joinCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID);

        assertThat(result.id()).isEqualTo(CARPOOL_ID);
        verify(carpoolPassengerRepository).save(org.mockito.ArgumentMatchers.any());
    }

    // ─── leave ──────────────────────────────────────────────────────────────

    @Test
    void leave_throws_when_user_not_passenger() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(ORG_ID)));
        when(carpoolPassengerRepository.existsActive(CARPOOL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> carpoolUseCase.leaveCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passager");
    }

    @Test
    void leave_removes_passenger_when_active() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(ORG_ID)));
        when(carpoolPassengerRepository.existsActive(CARPOOL_ID, USER_ID)).thenReturn(true);

        carpoolUseCase.leaveCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID);

        verify(carpoolPassengerRepository).removePassenger(CARPOOL_ID, USER_ID);
    }

    // ─── update / cancel ──────────────────────────────────────────────────────

    @Test
    void update_throws_when_caller_is_not_the_driver() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(ORG_ID)));

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        assertThatThrownBy(() -> carpoolUseCase.updateCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conducteur");
    }

    @Test
    void cancel_cancels_carpool_and_removes_passengers_for_driver() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(USER_ID)));

        carpoolUseCase.cancelCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID);

        verify(carpoolPassengerRepository).removeAllByCarpoolId(CARPOOL_ID);
        verify(carpoolRepository).cancelByDriverIdAndActivityId(USER_ID, ACTIVITY_ID);
    }

    // ─── list (extra branches) ───────────────────────────────────────────────

    @Test
    void list_returns_none_role_when_no_carpools_exist() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of());

        ActivityCarpoolsResponseDto result = carpoolUseCase.listCarpools(ACTIVITY_ID, USER_ID);

        assertThat(result.userRole()).isEqualTo("NONE");
        assertThat(result.userCarpoolId()).isNull();
        assertThat(result.carpools()).isEmpty();
    }

    @Test
    void list_returns_passenger_role_when_user_is_a_passenger() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(activeCarpool(ORG_ID)));

        CarpoolPassenger passenger = CarpoolPassenger.builder()
                .carpoolId(CARPOOL_ID).passengerId(USER_ID)
                .joinedAt(LocalDateTime.of(2026, Month.MARCH, 20, 9, 0)).build();
        when(carpoolPassengerRepository.findActiveByPassengerIdAndCarpoolIds(USER_ID, List.of(CARPOOL_ID)))
                .thenReturn(Optional.of(passenger));
        when(userRepository.findById(ORG_ID)).thenReturn(Optional.of(mock(User.class)));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(CARPOOL_ID))
                .thenReturn(List.of());

        ActivityCarpoolsResponseDto result = carpoolUseCase.listCarpools(ACTIVITY_ID, USER_ID);

        assertThat(result.userRole()).isEqualTo("PASSENGER");
        assertThat(result.userCarpoolId()).isEqualTo(CARPOOL_ID);
    }

    // ─── create (extra branches) ─────────────────────────────────────────────

    @Test
    void create_succeeds_for_organizer_without_subscription_check() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findActiveByDriverIdAndActivityId(ORG_ID, ACTIVITY_ID))
                .thenReturn(Optional.empty());
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID)).thenReturn(List.of());
        when(carpoolRepository.save(org.mockito.ArgumentMatchers.any(Carpool.class)))
                .thenReturn(activeCarpool(ORG_ID));
        when(userRepository.findById(ORG_ID)).thenReturn(Optional.of(mock(User.class)));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(CARPOOL_ID))
                .thenReturn(List.of());

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        CarpoolDetailDto result = carpoolUseCase.createCarpool(ACTIVITY_ID, ORG_ID, dto);

        assertThat(result.driverId()).isEqualTo(ORG_ID);
        verify(subscriptionRepository, org.mockito.Mockito.never())
                .existsByActivityIdAndUserId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_throws_when_user_already_has_driver_role() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(carpoolRepository.findActiveByDriverIdAndActivityId(USER_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(activeCarpool(USER_ID)));

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        assertThatThrownBy(() -> carpoolUseCase.createCarpool(ACTIVITY_ID, USER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("covoiturage");
    }

    // ─── update (extra branches) ─────────────────────────────────────────────

    @Test
    void update_succeeds_for_the_driver() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(USER_ID)));
        when(carpoolPassengerRepository.countActive(CARPOOL_ID)).thenReturn(1);
        when(carpoolRepository.save(org.mockito.ArgumentMatchers.any(Carpool.class)))
                .thenReturn(activeCarpool(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(carpoolPassengerRepository.findAllActivePassengersByCarpoolId(CARPOOL_ID))
                .thenReturn(List.of());

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        CarpoolDetailDto result = carpoolUseCase.updateCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID, dto);

        assertThat(result.id()).isEqualTo(CARPOOL_ID);
        verify(carpoolRepository).save(org.mockito.ArgumentMatchers.any(Carpool.class));
    }

    @Test
    void update_throws_when_carpool_is_not_active() {
        Carpool cancelled = Carpool.builder()
                .id(CARPOOL_ID).activityId(ACTIVITY_ID).driverId(USER_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3)
                .status(CarpoolStatus.CANCELLED).build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(cancelled));

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        assertThatThrownBy(() -> carpoolUseCase.updateCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("actif");
    }

    @Test
    void update_throws_when_new_capacity_is_below_current_passenger_count() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(USER_ID)));
        when(carpoolPassengerRepository.countActive(CARPOOL_ID)).thenReturn(3);

        CarpoolRequestDto dto = new CarpoolRequestDto(LocalTime.of(9, 0), 2);
        assertThatThrownBy(() -> carpoolUseCase.updateCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passagers actuels");
    }

    // ─── cancel (extra branches) ─────────────────────────────────────────────

    @Test
    void cancel_throws_when_caller_is_not_the_driver() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(activeCarpool(ORG_ID)));

        assertThatThrownBy(() -> carpoolUseCase.cancelCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conducteur");
    }

    @Test
    void cancel_throws_when_carpool_is_already_cancelled() {
        Carpool cancelled = Carpool.builder()
                .id(CARPOOL_ID).activityId(ACTIVITY_ID).driverId(USER_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3)
                .status(CarpoolStatus.CANCELLED).build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> carpoolUseCase.cancelCarpoolByDriver(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("actif");
    }

    // ─── join (extra branches) ───────────────────────────────────────────────

    @Test
    void join_throws_when_carpool_belongs_to_different_activity() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(true);
        Carpool wrongActivity = Carpool.builder()
                .id(CARPOOL_ID).activityId(999L).driverId(ORG_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3)
                .status(CarpoolStatus.ACTIVE).build();
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(wrongActivity));

        assertThatThrownBy(() -> carpoolUseCase.joinCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void join_throws_when_carpool_is_full_via_policy() {
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(offSiteFutureActivity()));
        when(subscriptionRepository.existsByActivityIdAndUserId(ACTIVITY_ID, USER_ID)).thenReturn(true);
        Carpool fullCarpool = Carpool.builder()
                .id(CARPOOL_ID).activityId(ACTIVITY_ID).driverId(ORG_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(2)
                .status(CarpoolStatus.ACTIVE).build();
        when(carpoolRepository.findById(CARPOOL_ID)).thenReturn(Optional.of(fullCarpool));
        when(carpoolRepository.findActiveByDriverIdAndActivityId(USER_ID, ACTIVITY_ID))
                .thenReturn(Optional.empty());
        when(carpoolRepository.findAllActiveByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(fullCarpool));
        when(carpoolPassengerRepository.findActiveByPassengerIdAndCarpoolIds(USER_ID, List.of(CARPOOL_ID)))
                .thenReturn(Optional.empty());
        when(carpoolPassengerRepository.countActive(CARPOOL_ID)).thenReturn(2);

        assertThatThrownBy(() -> carpoolUseCase.joinCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("covoiturage");
    }

    // ─── leave (extra branches) ──────────────────────────────────────────────

    @Test
    void leave_throws_when_activity_is_already_past() {
        Activity past = Activity.builder()
                .id(ACTIVITY_ID).title("Passée").capacity(10)
                .location(Location.builder().locationType(LocationType.OFF_SITE)
                        .street("r").postalCode("p").city("c").build())
                .typeId(2L).organizerId(ORG_ID)
                .date(LocalDate.of(2026, Month.MARCH, 20))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(past));

        assertThatThrownBy(() -> carpoolUseCase.leaveCarpool(ACTIVITY_ID, CARPOOL_ID, USER_ID))
                .isInstanceOf(BusinessException.class);
    }
}
