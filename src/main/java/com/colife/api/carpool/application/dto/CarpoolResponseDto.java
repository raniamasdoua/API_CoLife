package com.colife.api.carpool.application.dto;

import java.time.LocalTime;
import java.util.UUID;

public record CarpoolResponseDto(
        Long id,
        Long activityId,
        UUID driverId,
        LocalTime departureTime,
        int maxPassengers
) {
}
