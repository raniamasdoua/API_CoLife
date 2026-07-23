package com.colife.api.carpool.application.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.colife.api.carpool.domain.CarpoolStatus;

public record CarpoolDetailDto(
        Long id,
        Long activityId,
        UUID driverId,
        String driverName,
        LocalTime departureTime,
        int maxPassengers,
        int passengerCount,
        int availableSeats,
        CarpoolStatus status,
        List<CarpoolPassengerSummaryDto> passengers
) {
}
