package com.example.api.activity.infrastructure;

import com.example.api.activityType.infrastructure.ActivityTypeEntity;
import com.example.api.user.infrastructure.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "activities")
public class ActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private int capacity;

    @Embedded
    private LocationEmbeddable location;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private ActivityTypeEntity type;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private UserEntity organizer;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private boolean isDeleted;

}
