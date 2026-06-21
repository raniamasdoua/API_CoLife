package com.example.api.carpool.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class Carpool {
    private Long id;
    private Long activityId;
    private UUID driverId;
    private LocalTime departureTime;
    private int maxPassengers;
    private CarpoolStatus status;
}
