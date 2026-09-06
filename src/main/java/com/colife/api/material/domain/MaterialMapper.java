package com.colife.api.material.domain;

import com.colife.api.material.infrastructure.MaterialEntity;

public final class MaterialMapper {

    private MaterialMapper() {
    }

    public static MaterialProposal toDomain(MaterialEntity entity) {
        return MaterialProposal.builder()
                .id(entity.getId())
                .activityId(entity.getActivityId())
                .proposedBy(entity.getProposedBy())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static MaterialEntity toEntity(MaterialProposal proposal) {
        MaterialEntity entity = new MaterialEntity();
        entity.setId(proposal.getId());
        entity.setActivityId(proposal.getActivityId());
        entity.setProposedBy(proposal.getProposedBy());
        entity.setDescription(proposal.getDescription());
        entity.setQuantity(proposal.getQuantity());
        entity.setCreatedAt(proposal.getCreatedAt());
        return entity;
    }
}
