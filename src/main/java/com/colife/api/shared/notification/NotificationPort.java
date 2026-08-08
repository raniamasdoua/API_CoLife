package com.colife.api.shared.notification;

/**
 * Envoi de notifications aux utilisateurs (email pour l'instant, autres canaux possibles plus tard).
 */
public interface NotificationPort {

    void send(String to, String subject, String body);
}
