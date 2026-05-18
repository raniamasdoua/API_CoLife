package com.example.api.carpool.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "carpools")
@Getter
@Setter
@NoArgsConstructor
public class CarpoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private Long driverId;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private int maxPassengers;
}
