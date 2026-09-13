package com.colife.api.material.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MaterialRequestDto(
        @NotBlank(message = "est obligatoire") String description,
        @Min(value = 1, message = "doit être supérieure ou égale à 1") int quantity
) {
}
