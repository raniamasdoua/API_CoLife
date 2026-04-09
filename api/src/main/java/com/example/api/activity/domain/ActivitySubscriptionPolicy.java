package com.example.api.activity.domain;

import com.example.api.shared.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Règles métier d'inscription à une activité.
 */
public final class ActivitySubscriptionPolicy {

    private ActivitySubscriptionPolicy() {
    }

    /**
     * Vérifie que l'activité est ouverte à l'inscription (pas passée, pas en cours).
     */
    public static void validateActivityIsOpenForSubscription(Activity activity, LocalDate today, LocalTime now) {
        LocalDate activityDate = activity.getDate();
        LocalTime activityStart = activity.getStartTime();

        if (activityDate.isBefore(today)) {
            throw new BusinessException("Impossible de s'inscrire à une activité déjà passée");
        }
        if (activityDate.isEqual(today) && !now.isBefore(activityStart)) {
            throw new BusinessException("Impossible de s'inscrire à une activité en cours ou déjà terminée aujourd'hui");
        }
    }
}

