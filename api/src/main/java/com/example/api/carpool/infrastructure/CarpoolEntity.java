package com.example.api.carpool.infrastructure;

import com.example.api.carpool.domain.CarpoolStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

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
    private UUID driverId;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private int maxPassengers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarpoolStatus status = CarpoolStatus.ACTIVE;
}
