package com.example.api.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Corps de la requête PATCH /user/{id}/password.
 * Requiert le mot de passe actuel pour vérification et le nouveau mot de passe.
 */
public record ChangePasswordRequestDto(

        @NotBlank(message = "Le mot de passe actuel est requis")
        String currentPassword,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!?.]).{12,}$",
                message = "Le mot de passe doit contenir au moins 12 caractères et inclure une majuscule, une minuscule, un chiffre et un caractère spécial"
        )
        String newPassword
) {}
