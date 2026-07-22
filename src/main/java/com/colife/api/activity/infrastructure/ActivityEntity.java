package com.colife.api.activity.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

import com.colife.api.activityType.infrastructure.ActivityTypeEntity;
import com.colife.api.user.infrastructure.UserEntity;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
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
