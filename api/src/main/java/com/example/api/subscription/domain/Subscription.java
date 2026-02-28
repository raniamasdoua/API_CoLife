package com.example.api.subscription.domain;

import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public class Subscription {
    private Long id;
    private Long activityId;
    private Long userId;
    private LocalDateTime subscribedAt;
}
