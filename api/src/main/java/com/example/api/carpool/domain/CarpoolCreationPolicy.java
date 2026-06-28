package com.example.api.carpool.domain;

import com.example.api.shared.exception.BusinessException;

import java.time.LocalTime;

/**
 * Règles métier de création d'une proposition de covoiturage.
 */
public final class CarpoolCreationPolicy {

    private CarpoolCreationPolicy() {
    }

    public static void validate(int maxPassengers, LocalTime departureTime, LocalTime activityStartTime) {
        if (maxPassengers < 1) {
            throw new BusinessException("Le nombre de places passagers doit être supérieur ou égal à 1");
        }
        if (departureTime == null) {
            throw new BusinessException("L'heure de départ est obligatoire");
        }
        if (!departureTime.isBefore(activityStartTime)) {
            throw new BusinessException("L'heure de départ doit être avant le début de l'activité");
        }
    }
}
