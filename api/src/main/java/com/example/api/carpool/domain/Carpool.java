package com.example.api.carpool.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class Carpool {
    private Long id;
    private Long activityId;
    private Long driverId;
    private LocalTime departureTime;
    private int maxPassengers;
    private CarpoolStatus status;
}
