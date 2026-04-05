package com.example.api.activity.domain;

import com.example.api.shared.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Règles métier de modification d'une activité.
 */
public final class ActivityUpdatePolicy {

    private ActivityUpdatePolicy() {
    }

    /**
     * Vérifie que l'activité est encore modifiable (pas passée, pas en cours).
     */
    public static void validateActivityIsModifiable(Activity activity, LocalDate today, LocalTime now) {
        LocalDate activityDate = activity.getDate();
        LocalTime activityStart = activity.getStartTime();

        if (activityDate.isBefore(today)) {
            throw new BusinessException("Impossible de modifier une activité déjà passée");
        }
        if (activityDate.isEqual(today) && !now.isBefore(activityStart)) {
            throw new BusinessException("Impossible de modifier une activité en cours ou déjà terminée aujourd'hui");
        }
    }

    /**
     * Valide le nouveau créneau proposé pour l'activité.
     */
    public static void validateNewSlot(
            LocalDate newDate, LocalDate today, LocalTime now,
            LocalTime startTime, LocalTime endTime,
            int capacity, int participantCount) {

        if (newDate.isBefore(today)) {
            throw new BusinessException("La nouvelle date ne peut pas être dans le passé");
        }
        if (newDate.isEqual(today) && startTime.isBefore(now)) {
            throw new BusinessException("Le nouveau créneau horaire est déjà passé pour aujourd'hui");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("L'heure de fin doit être après l'heure de début");
        }
        if (capacity < participantCount) {
            throw new BusinessException(
                    "La capacité ne peut pas être inférieure au nombre de participants actuels (" + participantCount + ")");
        }
    }
}
