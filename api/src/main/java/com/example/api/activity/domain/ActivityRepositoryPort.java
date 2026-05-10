package com.example.api.activity.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepositoryPort {
    Optional<Activity> findById(Long id);

    Activity save(Activity activity);

    Activity update(Activity activity);

    List<Activity> findAll();

    List<Activity> findByOrganizerId(Long organizerId);

    List<Activity> findByOrganizerIdNot(Long organizerId);

    /**
     * Retourne les activités disponibles pour inscription pour un utilisateur :
     * - non supprimées
     * - non passées / non en cours (date > today, ou date == today et startTime > now)
     * - dont l'utilisateur n'est pas l'organisateur
     */
    List<Activity> findAvailableForUser(Long userId, LocalDate today, LocalTime now);

    /**
     * Activités où l'utilisateur a une inscription mais n'est pas l'organisateur.
     */
    List<Activity> findSubscribedAsNonOrganizer(Long userId);

    /**
     * Vérifie si l'organisateur a déjà une activité non supprimée le même jour avec un créneau qui chevauche [start, end].
     */
    boolean existsOverlappingForOrganizer(Long organizerId, LocalDate date, LocalTime start, LocalTime end);

    /**
     * Vérifie si l'un des utilisateurs donnés est organisateur d'une autre activité (hors excludeActivityId)
     * le même jour avec un créneau qui chevauche [start, end].
     */
    boolean existsOverlappingForUsersAsOrganizer(List<Long> userIds, LocalDate date, LocalTime start, LocalTime end, Long excludeActivityId);

    /**
     * Marque l'activité comme supprimée (soft delete).
     */
    void softDelete(Long activityId);

    /**
     * Indique si au moins une activité non supprimée référence ce type d'activité.
     */
    boolean existsNonDeletedByActivityTypeId(Long activityTypeId);
}
