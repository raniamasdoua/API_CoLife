package com.example.api.carpool.application.dto;

import java.time.LocalTime;

public record CarpoolResponseDto(
        Long id,
        Long activityId,
        Long driverId,
        LocalTime departureTime,
        int maxPassengers
) {
}
