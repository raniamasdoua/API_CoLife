package com.example.api.user.application.dto;

import com.example.api.user.domain.Role;

import java.time.LocalDate;

/**
 * Réponse complète du profil utilisateur.
 * Renvoyée par GET /user/{id} et PATCH /user/{id}/profile.
 */
public record UserResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        String bio,
        String phone,
        String address,
        LocalDate createdAt
) {}
