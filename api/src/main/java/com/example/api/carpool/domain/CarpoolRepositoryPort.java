package com.example.api.carpool.domain;

import java.util.List;
import java.util.Optional;

public interface CarpoolRepositoryPort {

    Carpool save(Carpool carpool);

    Optional<Carpool> findById(Long id);

    /** Retourne le premier covoiturage actif de l'activité (pour ActivityResponseDto). */
    Optional<Carpool> findByActivityId(Long activityId);

    /** Retourne tous les covoiturages actifs d'une activité. */
    List<Carpool> findAllActiveByActivityId(Long activityId);

    /** Retourne le covoiturage actif dont le conducteur est l'utilisateur donné. */
    Optional<Carpool> findActiveByDriverIdAndActivityId(Long driverId, Long activityId);

    /** Annule le covoiturage actif d'un conducteur pour une activité. */
    void cancelByDriverIdAndActivityId(Long driverId, Long activityId);

    /** Annule tous les covoiturages actifs d'une activité (suppression de l'activité). */
    void cancelAllByActivityId(Long activityId);
}
