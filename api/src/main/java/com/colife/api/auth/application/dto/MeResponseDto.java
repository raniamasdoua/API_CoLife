package com.colife.api.auth.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.colife.api.user.domain.Role;

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
