package com.example.api.activity.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class Activity {
    private Long id;
    private String title;
    private String description;
    private int capacity;
    private Location location;
    private Long typeId;
    private UUID organizerId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean deleted;
}
