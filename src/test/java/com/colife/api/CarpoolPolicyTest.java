package com.colife.api;

import com.colife.api.carpool.domain.Carpool;
import com.colife.api.carpool.domain.CarpoolCreationPolicy;
import com.colife.api.carpool.domain.CarpoolJoinPolicy;
import com.colife.api.carpool.domain.CarpoolStatus;
import com.colife.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarpoolPolicyTest {

    private static final UUID DRIVER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID  = UUID.fromString("22222222-0000-0000-0000-000000000002");

    // ─── CarpoolCreationPolicy ────────────────────────────────────────────────

    @Test
    void creation_valid_when_all_conditions_met() {
        assertThatCode(() -> CarpoolCreationPolicy.validate(2, LocalTime.of(8, 0), LocalTime.of(9, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void creation_throws_when_maxPassengers_is_zero() {
        LocalTime dep = LocalTime.of(8, 0);
        LocalTime act = LocalTime.of(9, 0);
        assertThatThrownBy(() -> CarpoolCreationPolicy.validate(0, dep, act))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1");
    }

    @Test
    void creation_throws_when_departureTime_is_null() {
        LocalTime act = LocalTime.of(9, 0);
        assertThatThrownBy(() -> CarpoolCreationPolicy.validate(2, null, act))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obligatoire");
    }

    @Test
    void creation_throws_when_departureTime_equals_activity_start() {
        LocalTime time = LocalTime.of(9, 0);
        assertThatThrownBy(() -> CarpoolCreationPolicy.validate(2, time, time))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("avant");
    }

    @Test
    void creation_throws_when_departureTime_is_after_activity_start() {
        LocalTime dep = LocalTime.of(10, 0);
        LocalTime act = LocalTime.of(9, 0);
        assertThatThrownBy(() -> CarpoolCreationPolicy.validate(2, dep, act))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("avant");
    }

    // ─── CarpoolJoinPolicy ────────────────────────────────────────────────────

    private Carpool activeCarpool() {
        return Carpool.builder()
                .id(1L).activityId(1L).driverId(DRIVER_ID)
                .departureTime(LocalTime.of(8, 0)).maxPassengers(3)
                .status(CarpoolStatus.ACTIVE).build();
    }

    @Test
    void join_valid_when_all_conditions_met() {
        assertThatCode(() -> CarpoolJoinPolicy.validate(activeCarpool(), OTHER_ID, false, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void join_throws_when_carpool_is_cancelled() {
        Carpool cancelled = Carpool.builder()
                .id(1L).activityId(1L).driverId(DRIVER_ID)
                .departureTime(LocalTime.of(8, 0)).maxPassengers(3)
                .status(CarpoolStatus.CANCELLED).build();
        assertThatThrownBy(() -> CarpoolJoinPolicy.validate(cancelled, OTHER_ID, false, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("annul");
    }

    @Test
    void join_throws_when_user_is_the_driver() {
        Carpool carpool = activeCarpool();
        assertThatThrownBy(() -> CarpoolJoinPolicy.validate(carpool, DRIVER_ID, false, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conducteur");
    }

    @Test
    void join_throws_when_user_already_has_a_carpool_role() {
        Carpool carpool = activeCarpool();
        assertThatThrownBy(() -> CarpoolJoinPolicy.validate(carpool, OTHER_ID, true, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("covoiturage");
    }

    @Test
    void join_throws_when_carpool_is_full() {
        Carpool carpool = activeCarpool();
        assertThatThrownBy(() -> CarpoolJoinPolicy.validate(carpool, OTHER_ID, false, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("covoiturage");
    }
}
