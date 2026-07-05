package com.colife.api.carpool.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colife.api.carpool.domain.CarpoolStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarpoolJpaRepository extends JpaRepository<CarpoolEntity, Long> {

    Optional<CarpoolEntity> findFirstByActivityIdAndStatusOrderByIdAsc(Long activityId, CarpoolStatus status);

    List<CarpoolEntity> findByActivityIdAndStatus(Long activityId, CarpoolStatus status);

    Optional<CarpoolEntity> findByDriverIdAndActivityIdAndStatus(UUID driverId, Long activityId, CarpoolStatus status);

    @Modifying
    @Query("UPDATE CarpoolEntity c SET c.status = 'CANCELLED' WHERE c.driverId = :driverId AND c.activityId = :activityId AND c.status = 'ACTIVE'")
    void cancelByDriverIdAndActivityId(@Param("driverId") UUID driverId, @Param("activityId") Long activityId);

    @Modifying
    @Query("UPDATE CarpoolEntity c SET c.status = 'CANCELLED' WHERE c.activityId = :activityId AND c.status = 'ACTIVE'")
    void cancelAllByActivityId(@Param("activityId") Long activityId);
}
