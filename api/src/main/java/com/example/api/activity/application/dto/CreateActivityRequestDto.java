package com.example.api.activity.application.dto;

import com.example.api.activity.domain.LocationType;
import com.example.api.carpool.application.dto.CarpoolRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateActivityRequestDto(
        @NotBlank(message = "est obligatoire") String title,
        String description,
        @NotNull(message = "est obligatoire") Long activityTypeId,
        @NotNull(message = "est obligatoire") @FutureOrPresent(message = "doit être aujourd'hui ou dans le futur") LocalDate date,
        @NotNull(message = "est obligatoire") LocalTime startTime,
        @NotNull(message = "est obligatoire") LocalTime endTime,
        @Positive(message = "doit être strictement positive") int capacity,
        @NotNull(message = "est obligatoire") @Valid LocationDto location,
        LocationType locationType,
        @Valid CarpoolRequestDto carpool
) {
}
