package com.example.api;

import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.activity.domain.Location;
import com.example.api.activity.domain.LocationType;
import com.example.api.carpool.application.CarpoolUseCase;
import com.example.api.carpool.application.dto.ActivityCarpoolsResponseDto;
import com.example.api.carpool.application.dto.CarpoolDetailDto;
import com.example.api.carpool.application.dto.CarpoolRequestDto;
import com.example.api.carpool.domain.Carpool;
import com.example.api.carpool.domain.CarpoolPassengerRepositoryPort;
import com.example.api.carpool.domain.CarpoolRepositoryPort;
import com.example.api.carpool.domain.CarpoolStatus;
import com.example.api.shared.exception.BusinessException;
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
                .date(LocalDate.of(2026, 3, 30))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .deleted(false).locationType(LocationType.OFF_SITE).build();
    }

    private Activity onSiteActivity() {
        return Activity.builder()
                .id(ACTIVITY_ID).title("Réunion").capacity(10)
                .location(Location.builder().locationType(LocationType.ON_SITE).room("A1").build())
                .typeId(2L).organizerId(ORG_ID)
                .date(LocalDate.of(2026, 3, 30))
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
}
