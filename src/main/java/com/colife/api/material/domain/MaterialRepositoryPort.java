package com.colife.api.material.domain;

import java.util.List;
import java.util.Optional;

public interface MaterialRepositoryPort {

    MaterialProposal save(MaterialProposal proposal);

    Optional<MaterialProposal> findById(Long id);

    /** Retourne les propositions de matériel d'une activité, dans l'ordre de création. */
    List<MaterialProposal> findAllByActivityId(Long activityId);

    void deleteById(Long id);
}
