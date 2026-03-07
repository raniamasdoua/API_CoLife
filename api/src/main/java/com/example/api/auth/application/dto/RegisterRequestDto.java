package com.example.api.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @NotBlank
        @Email
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@entreprise\\.com$",
                message = "L'adresse e-mail doit être au format @entreprise.com"
        )
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!?.]).{12,}$",
                message = "Le mot de passe doit contenir au moins 12 caractères et inclure une majuscule, une minuscule, un chiffre et un caractère spécial"
        )
        String password
) {}