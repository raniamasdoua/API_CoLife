package com.colife.api.material.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MaterialProposal {
    private Long id;
    private Long activityId;
    private UUID proposedBy;
    private String description;
    private int quantity;
    private LocalDateTime createdAt;
}
