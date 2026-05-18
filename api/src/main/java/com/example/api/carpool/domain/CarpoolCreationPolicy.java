package com.example.api.carpool.domain;

import com.example.api.shared.exception.BusinessException;

/**
 * Règles métier de création d'une proposition de covoiturage.
 */
public final class CarpoolCreationPolicy {

    private CarpoolCreationPolicy() {
    }

    public static void validate(int maxPassengers) {
        if (maxPassengers < 1) {
            throw new BusinessException("Le nombre de places passagers doit être supérieur ou égal à 1");
        }
    }
}
