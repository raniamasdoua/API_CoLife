package com.colife.api.carpool.application.dto;

import java.util.List;

/**
 * Résumé des covoiturages d'une activité pour un utilisateur authentifié.
 */
public record ActivityCarpoolsResponseDto(
        List<CarpoolDetailDto> carpools,
        /** "DRIVER", "PASSENGER" ou "NONE" */
        String userRole,
        /** ID du covoiturage auquel l'utilisateur est lié (null si NONE). */
        Long userCarpoolId
) {
}
