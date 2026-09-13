package com.colife.api.material.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialJpaRepository extends JpaRepository<MaterialEntity, Long> {

    List<MaterialEntity> findByActivityIdAndDeletedFalseOrderByCreatedAtAsc(Long activityId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MaterialEntity m set m.deleted = true where m.activityId = :activityId")
    void softDeleteAllByActivityId(@Param("activityId") Long activityId);
}
