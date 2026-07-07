package com.colife.api.carpool.application.dto;

import java.util.UUID;

public record CarpoolPassengerSummaryDto(
        UUID userId,
        String fullName
) {
}
