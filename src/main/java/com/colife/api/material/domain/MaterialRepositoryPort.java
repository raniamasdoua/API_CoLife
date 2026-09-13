package com.colife.api.material.domain;

import java.util.List;
import java.util.Optional;

public interface MaterialRepositoryPort {

    MaterialProposal save(MaterialProposal proposal);

    Optional<MaterialProposal> findById(Long id);

    /** Retourne les propositions de matériel d'une activité, dans l'ordre de création. */
    List<MaterialProposal> findAllByActivityId(Long activityId);

    void deleteById(Long id);

    /** Marque comme supprimées toutes les propositions de matériel d'une activité (suppression en cascade lors du soft delete de l'activité). */
    void softDeleteAllByActivityId(Long activityId);
}
