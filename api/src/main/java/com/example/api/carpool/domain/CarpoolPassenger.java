package com.example.api.carpool.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CarpoolPassenger {
    private Long id;
    private Long carpoolId;
    private UUID passengerId;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
