package com.example.api.activity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LocationDto(
        @NotBlank(message = "est obligatoire") String street,
        String complement,
        @NotBlank(message = "est obligatoire") String postalCode,
        @NotBlank(message = "est obligatoire") String city
) {
}
