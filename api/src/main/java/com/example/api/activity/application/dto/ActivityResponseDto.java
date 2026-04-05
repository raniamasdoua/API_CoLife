package com.example.api.activity.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

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
        String organizerName
) {
}
