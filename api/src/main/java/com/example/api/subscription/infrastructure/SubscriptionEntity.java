package com.example.api.subscription.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long activityId;
    private Long userId;
    private LocalDateTime subscribedAt;

    /** Renseigné lors d'une désinscription volontaire (traçabilité, pas de suppression physique). */
    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
}
