package com.colife.api.carpool.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarpoolPassengerRepositoryPort {

    CarpoolPassenger save(CarpoolPassenger passenger);

    int countActive(Long carpoolId);

    boolean existsActive(Long carpoolId, UUID passengerId);

    /**
     * Retourne le covoiturage dont l'utilisateur est passager actif,
     * parmi une liste de covoiturages (tous liés à la même activité).
     */
    Optional<CarpoolPassenger> findActiveByPassengerIdAndCarpoolIds(UUID passengerId, List<Long> carpoolIds);

    /**
     * Retourne tous les passagers actifs d'un covoiturage.
     */
    List<CarpoolPassenger> findAllActivePassengersByCarpoolId(Long carpoolId);

    /**
     * Retire un passager d'un covoiturage (soft-delete).
     */
    void removePassenger(Long carpoolId, UUID passengerId);

    /**
     * Retire tous les passagers actifs d'un covoiturage (annulation conducteur).
     */
    void removeAllByCarpoolId(Long carpoolId);

    /**
     * Retire tous les passagers actifs de plusieurs covoiturages (suppression activité).
     */
    void removeAllByCarpoolIds(List<Long> carpoolIds);
}
