package com.colife.api.material.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialJpaRepository extends JpaRepository<MaterialEntity, Long> {

    List<MaterialEntity> findByActivityIdOrderByCreatedAtAsc(Long activityId);
}
