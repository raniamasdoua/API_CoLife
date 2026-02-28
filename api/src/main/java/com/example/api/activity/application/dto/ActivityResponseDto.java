package com.example.api.activity.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ActivityResponseDto (
        Long id,
        String title,
        String description,
        int capacity,
        LocationDto location,
        ActivityTypeDto activityType,
        LocalDate dateTime,
        LocalTime startTime,
        LocalTime endTime
){
}
