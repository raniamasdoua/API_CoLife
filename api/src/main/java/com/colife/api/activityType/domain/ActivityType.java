package com.colife.api.activityType.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityType {
    private Long id;
    private String name;
    @Builder.Default
    private boolean deleted = false;
}
