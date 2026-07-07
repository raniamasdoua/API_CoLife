package com.colife.api;

import com.colife.api.carpool.domain.Carpool;
import com.colife.api.carpool.domain.CarpoolMapper;
import com.colife.api.carpool.domain.CarpoolStatus;
import com.colife.api.carpool.infrastructure.CarpoolEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CarpoolMapperTest {

    private static final UUID DRIVER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    // ─── toDomain ─────────────────────────────────────────────────────────────

    @Test
    void toDomain_maps_all_fields_correctly() {
        CarpoolEntity entity = new CarpoolEntity();
        entity.setId(1L);
        entity.setActivityId(100L);
        entity.setDriverId(DRIVER_ID);
        entity.setDepartureTime(LocalTime.of(9, 0));
        entity.setMaxPassengers(3);
        entity.setStatus(CarpoolStatus.ACTIVE);

        Carpool carpool = CarpoolMapper.toDomain(entity);

        assertThat(carpool.getId()).isEqualTo(1L);
        assertThat(carpool.getActivityId()).isEqualTo(100L);
        assertThat(carpool.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(carpool.getDepartureTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(carpool.getMaxPassengers()).isEqualTo(3);
        assertThat(carpool.getStatus()).isEqualTo(CarpoolStatus.ACTIVE);
    }

    @Test
    void toDomain_maps_cancelled_status() {
        CarpoolEntity entity = new CarpoolEntity();
        entity.setId(2L);
        entity.setActivityId(200L);
        entity.setDriverId(DRIVER_ID);
        entity.setDepartureTime(LocalTime.of(8, 30));
        entity.setMaxPassengers(2);
        entity.setStatus(CarpoolStatus.CANCELLED);

        Carpool carpool = CarpoolMapper.toDomain(entity);

        assertThat(carpool.getStatus()).isEqualTo(CarpoolStatus.CANCELLED);
    }

    @Test
    void toDomain_defaults_status_to_ACTIVE_when_null() {
        CarpoolEntity entity = new CarpoolEntity();
        entity.setId(3L);
        entity.setActivityId(300L);
        entity.setDriverId(DRIVER_ID);
        entity.setDepartureTime(LocalTime.of(7, 0));
        entity.setMaxPassengers(4);
        entity.setStatus(null);

        Carpool carpool = CarpoolMapper.toDomain(entity);

        assertThat(carpool.getStatus()).isEqualTo(CarpoolStatus.ACTIVE);
    }

    // ─── toEntity ─────────────────────────────────────────────────────────────

    @Test
    void toEntity_maps_all_fields_correctly() {
        Carpool carpool = Carpool.builder()
                .id(1L).activityId(100L).driverId(DRIVER_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3)
                .status(CarpoolStatus.CANCELLED).build();

        CarpoolEntity entity = CarpoolMapper.toEntity(carpool);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getActivityId()).isEqualTo(100L);
        assertThat(entity.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(entity.getDepartureTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(entity.getMaxPassengers()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(CarpoolStatus.CANCELLED);
    }

    @Test
    void toEntity_defaults_status_to_ACTIVE_when_null() {
        Carpool carpool = Carpool.builder()
                .id(1L).activityId(100L).driverId(DRIVER_ID)
                .departureTime(LocalTime.of(9, 0)).maxPassengers(3)
                .status(null).build();

        CarpoolEntity entity = CarpoolMapper.toEntity(carpool);

        assertThat(entity.getStatus()).isEqualTo(CarpoolStatus.ACTIVE);
    }
}
