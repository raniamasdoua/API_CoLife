package com.example.api.activityType.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_types")
@Getter
@Setter
@NoArgsConstructor
public class ActivityTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Soft delete : type masqué des listes / création d'activités, mais conservé pour l'historique (FK). */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
