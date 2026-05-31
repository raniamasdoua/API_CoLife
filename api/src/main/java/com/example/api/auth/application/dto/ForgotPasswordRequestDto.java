package com.example.api.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequestDto(
        @NotBlank
        @Email
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@entreprise\\.com$",
                message = "L'adresse e-mail doit être au format @entreprise.com"
        )
        String email
) {}
