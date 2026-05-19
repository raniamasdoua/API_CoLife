package com.example.api.carpool.domain;

import com.example.api.carpool.infrastructure.CarpoolEntity;

public final class CarpoolMapper {

    private CarpoolMapper() {
    }

    public static Carpool toDomain(CarpoolEntity entity) {
        return Carpool.builder()
                .id(entity.getId())
                .activityId(entity.getActivityId())
                .driverId(entity.getDriverId())
                .departureTime(entity.getDepartureTime())
                .maxPassengers(entity.getMaxPassengers())
                .status(entity.getStatus() != null ? entity.getStatus() : CarpoolStatus.ACTIVE)
                .build();
    }

    public static CarpoolEntity toEntity(Carpool carpool) {
        CarpoolEntity entity = new CarpoolEntity();
        entity.setActivityId(carpool.getActivityId());
        entity.setDriverId(carpool.getDriverId());
        entity.setDepartureTime(carpool.getDepartureTime());
        entity.setMaxPassengers(carpool.getMaxPassengers());
        entity.setStatus(carpool.getStatus() != null ? carpool.getStatus() : CarpoolStatus.ACTIVE);
        return entity;
    }
}
