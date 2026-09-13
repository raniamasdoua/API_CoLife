package com.colife.api.activity.application.dto;

import java.util.List;

public record UserActivitiesDto(
        List<ActivityResponseDto> organized,
        List<ActivityResponseDto> registered
) {
}
