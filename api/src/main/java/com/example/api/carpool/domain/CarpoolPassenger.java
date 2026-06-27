package com.example.api.carpool.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CarpoolPassenger {
    private Long id;
    private Long carpoolId;
    private Long passengerId;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
