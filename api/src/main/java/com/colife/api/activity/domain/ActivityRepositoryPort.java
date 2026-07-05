package com.colife.api.activity.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepositoryPort {
    Optional<Activity> findById(Long id);

    Activity save(Activity activity);

    Activity update(Activity activity);

    List<Activity> findAll();

    List<Activity> findByOrganizerId(UUID organizerId);

    List<Activity> findByOrganizerIdNot(UUID organizerId);

    /**
     * Retourne les activités disponibles pour inscription pour un utilisateur :
     * - non supprimées
     * - non passées / non en cours (date > today, ou date == today et startTime > now)
     * - dont l'utilisateur n'est pas l'organisateur
     */
    List<Activity> findAvailableForUser(UUID userId, LocalDate today, LocalTime now);

    /**
     * Activités où l'utilisateur a une inscription mais n'est pas l'organisateur.
     */
    List<Activity> findSubscribedAsNonOrganizer(UUID userId);

    /**
     * Vérifie si l'organisateur a déjà une activité non supprimée le même jour avec un créneau qui chevauche [start, end].
     */
    boolean existsOverlappingForOrganizer(UUID organizerId, LocalDate date, LocalTime start, LocalTime end);

    /**
     * Vérifie si l'un des utilisateurs donnés est organisateur d'une autre activité (hors excludeActivityId)
     * le même jour avec un créneau qui chevauche [start, end].
     */
    boolean existsOverlappingForUsersAsOrganizer(List<UUID> userIds, LocalDate date, LocalTime start, LocalTime end, Long excludeActivityId);

    /**
     * Marque l'activité comme supprimée (soft delete).
     */
    void softDelete(Long activityId);

    /**
     * Indique si au moins une activité non supprimée référence ce type d'activité.
     */
    boolean existsNonDeletedByActivityTypeId(Long activityTypeId);
}
