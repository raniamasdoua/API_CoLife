package com.colife.api.carpool.domain;

import java.util.UUID;

import com.colife.api.shared.exception.BusinessException;

/**
 * Règles métier pour rejoindre une proposition de covoiturage existante.
 */
public final class CarpoolJoinPolicy {

    private CarpoolJoinPolicy() {
    }

    public static void validate(Carpool carpool, UUID userId, boolean userAlreadyHasCarpoolRole,
                                int currentPassengerCount) {
        if (carpool.getStatus() == CarpoolStatus.CANCELLED) {
            throw new BusinessException("Cette proposition de covoiturage est annulée");
        }
        if (carpool.getDriverId().equals(userId)) {
            throw new BusinessException("Vous êtes le conducteur de cette proposition");
        }
        if (userAlreadyHasCarpoolRole) {
            throw new BusinessException("Vous avez déjà un rôle de covoiturage pour cette activité");
        }
        if (currentPassengerCount >= carpool.getMaxPassengers()) {
            throw new BusinessException("Cette proposition de covoiturage est complète");
        }
    }
}
