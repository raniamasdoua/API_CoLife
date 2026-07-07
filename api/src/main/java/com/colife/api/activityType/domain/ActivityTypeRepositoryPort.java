package com.colife.api.activityType.domain;

import java.util.List;
import java.util.Optional;

public interface ActivityTypeRepositoryPort {
    ActivityType save(ActivityType activityType);

    /** Type actif uniquement (non supprimé logiquement). */
    Optional<ActivityType> findActiveById(Long id);

    /** Par id en base, y compris type soft-supprimé (résolution pour activités existantes). */
    Optional<ActivityType> findByIdIncludingDeleted(Long id);

    /** Types actifs, pour listes et sélecteurs. */
    List<ActivityType> findAllActive();

    /** Unicité de nom parmi les types actifs uniquement. */
    boolean existsActiveByNameIgnoreCase(String name);

    void softDeleteById(Long id);

    long countActive();
}
