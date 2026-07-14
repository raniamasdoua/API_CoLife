package com.colife.api.carpool.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarpoolPassengerJpaRepository extends JpaRepository<CarpoolPassengerEntity, Long> {

    int countByCarpoolIdAndLeftAtIsNull(Long carpoolId);

    boolean existsByCarpoolIdAndPassengerIdAndLeftAtIsNull(Long carpoolId, UUID passengerId);

    Optional<CarpoolPassengerEntity> findByCarpoolIdAndPassengerIdAndLeftAtIsNull(Long carpoolId, UUID passengerId);

    List<CarpoolPassengerEntity> findByCarpoolIdAndLeftAtIsNull(Long carpoolId);

    @Query("""
            SELECT cp FROM CarpoolPassengerEntity cp
            WHERE cp.carpoolId IN :carpoolIds
              AND cp.passengerId = :passengerId
              AND cp.leftAt IS NULL
            """)
    Optional<CarpoolPassengerEntity> findActiveByPassengerIdAndCarpoolIds(
            @Param("passengerId") UUID passengerId,
            @Param("carpoolIds") List<Long> carpoolIds);

    @Modifying
    @Query("UPDATE CarpoolPassengerEntity cp SET cp.leftAt = :now WHERE cp.carpoolId = :carpoolId AND cp.passengerId = :passengerId AND cp.leftAt IS NULL")
    void softDeleteByPassengerIdAndCarpoolId(
            @Param("carpoolId") Long carpoolId,
            @Param("passengerId") UUID passengerId,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE CarpoolPassengerEntity cp SET cp.leftAt = :now WHERE cp.carpoolId = :carpoolId AND cp.leftAt IS NULL")
    void softDeleteAllByCarpoolId(@Param("carpoolId") Long carpoolId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE CarpoolPassengerEntity cp SET cp.leftAt = :now WHERE cp.carpoolId IN :carpoolIds AND cp.leftAt IS NULL")
    void softDeleteAllByCarpoolIds(@Param("carpoolIds") List<Long> carpoolIds, @Param("now") LocalDateTime now);
}
