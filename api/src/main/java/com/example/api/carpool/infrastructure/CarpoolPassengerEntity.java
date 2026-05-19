package com.example.api.carpool.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "carpool_passengers")
@Getter
@Setter
@NoArgsConstructor
public class CarpoolPassengerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long carpoolId;

    @Column(nullable = false)
    private Long passengerId;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;
}
