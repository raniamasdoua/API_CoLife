package com.example.api.activity.domain;

import com.example.api.activityType.domain.ActivityType;

import java.time.LocalDate;
import java.time.LocalTime;
public class Activity {
    private Long id;
    private String title;
    private String description;
    private int capacity;
    private Location location;
    private Long type_id;
    private Long organizer_id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isDeleted; }
