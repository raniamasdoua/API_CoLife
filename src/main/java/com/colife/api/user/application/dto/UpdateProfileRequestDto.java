package com.colife.api.user.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Corps de la requête PATCH /user/{id}/profile.
 * Seuls bio, phone et address sont modifiables par l'utilisateur.
 * Les champs null sont ignorés (patch partiel).
 */
public record UpdateProfileRequestDto(

        @Size(max = 500, message = "La bio ne peut pas dépasser 500 caractères")
        String bio,

        @Size(max = 30, message = "Le numéro de téléphone ne peut pas dépasser 30 caractères")
        String phone,

        @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
        String address
) {}
