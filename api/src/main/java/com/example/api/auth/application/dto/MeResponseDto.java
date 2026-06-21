package com.example.api.auth.application.dto;

import com.example.api.user.domain.Role;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Réponse du endpoint GET /auth/me.
 * Contient toutes les informations du profil de l'utilisateur connecté.
 */
public record MeResponseDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role,
        String bio,
        String phone,
        String address,
        LocalDate createdAt
) {}
