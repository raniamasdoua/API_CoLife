package com.example.api.activityType.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityType {
    private Long id;
    private String name;
}
