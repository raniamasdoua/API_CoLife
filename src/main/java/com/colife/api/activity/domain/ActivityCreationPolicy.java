package com.colife.api.activity.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import com.colife.api.shared.exception.BusinessException;

/**
 * Règles métier de création d'une activité (hors existence du type, chevauchement avec d'autres activités).
 */
public final class ActivityCreationPolicy {

    private ActivityCreationPolicy() {
    }

    public static void validate(LocalDate activityDate, LocalDate today, LocalTime now,
                              int capacity, LocalTime startTime, LocalTime endTime) {
        if (activityDate.isBefore(today)) {
            throw new BusinessException("La date ne peut pas être dans le passé");
        }
        if (activityDate.isEqual(today) && startTime.isBefore(now)) {
            throw new BusinessException("Le créneau horaire est déjà passé pour aujourd'hui");
        }
        if (capacity <= 0) {
            throw new BusinessException("La capacité doit être strictement positive");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("L'heure de fin doit être après l'heure de début");
        }
    }
}
