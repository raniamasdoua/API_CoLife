package com.example.api.carpool.application.dto;

import com.example.api.carpool.domain.CarpoolStatus;

import java.time.LocalTime;
import java.util.List;

public record CarpoolDetailDto(
        Long id,
        Long activityId,
        Long driverId,
        String driverName,
        LocalTime departureTime,
        int maxPassengers,
        int passengerCount,
        int availableSeats,
        CarpoolStatus status,
        List<CarpoolPassengerSummaryDto> passengers
) {
}
