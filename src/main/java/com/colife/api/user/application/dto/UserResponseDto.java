package com.colife.api.user.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.colife.api.user.domain.Role;

/**
 * Réponse complète du profil utilisateur.
 * Renvoyée par GET /user/{id} et PATCH /user/{id}/profile.
 */
public record UserResponseDto(
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
