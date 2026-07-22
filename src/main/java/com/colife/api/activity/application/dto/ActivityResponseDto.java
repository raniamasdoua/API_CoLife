package com.colife.api.activity.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.colife.api.activity.domain.LocationType;
import com.colife.api.carpool.application.dto.CarpoolResponseDto;

public record ActivityResponseDto(
        Long id,
        String title,
        String description,
        int capacity,
        int participantCount,
        LocationDto location,
        ActivityTypeDto activityType,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String organizerName,
        boolean deleted,
        LocationType locationType,
        CarpoolResponseDto carpool
) {
}
