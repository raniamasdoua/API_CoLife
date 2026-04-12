package com.example.api.subscription.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SubscriptionRepositoryPort {
    /** Inscription encore active (pas désinscrit). */
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);

    void registerParticipant(Long activityId, Long userId);

    /**
     * Désinscription volontaire : renseigne {@code unsubscribed_at} sans supprimer la ligne.
     */
    void unsubscribeParticipant(Long activityId, Long userId);

    int countParticipants(Long activityId);

    /**
     * Retourne les identifiants de tous les participants inscrits à une activité.
     */
    List<Long> findUserIdsByActivityId(Long activityId);

    /**
     * Vérifie si l'un des utilisateurs donnés est inscrit (en tant que participant)
     * à une autre activité (hors excludeActivityId) le même jour avec un créneau qui chevauche [start, end].
     */
    boolean existsConflictingActivityForSubscribedUsers(
            List<Long> userIds,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            Long excludeActivityId);

    /**
     * Supprime toutes les inscriptions à une activité (désinscription des participants).
     */
    void deleteAllByActivityId(Long activityId);
}
