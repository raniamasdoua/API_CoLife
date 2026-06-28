package com.example.api.carpool.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CarpoolRequestDto(
        @NotNull(message = "est obligatoire") LocalTime departureTime,
        @Min(value = 1, message = "doit être supérieur ou égal à 1") int maxPassengers
) {
}
