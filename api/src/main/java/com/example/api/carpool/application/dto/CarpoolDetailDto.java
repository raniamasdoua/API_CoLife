package com.example.api.carpool.application.dto;

import com.example.api.carpool.domain.CarpoolStatus;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
