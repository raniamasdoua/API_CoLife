package com.example.api.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequestDto(
        @NotBlank
        @Email
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@company\\.com$",
                message = "L'adresse e-mail doit être au format @company.com"
        )
        String email,

        @NotBlank
        String password
) {}
