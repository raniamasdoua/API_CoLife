package com.colife.api.activityType.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityTypeRequestDto (
        @NotBlank(message = "est obligatoire")
        @Size(max = 60, message = "ne doit pas dépasser 60 caractères")
        String name
) {
}