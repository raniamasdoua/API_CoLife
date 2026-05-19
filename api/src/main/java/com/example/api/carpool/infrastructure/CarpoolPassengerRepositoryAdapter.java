package com.example.api.carpool.infrastructure;

import com.example.api.carpool.domain.CarpoolPassenger;
import com.example.api.carpool.domain.CarpoolPassengerRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class CarpoolPassengerRepositoryAdapter implements CarpoolPassengerRepositoryPort {

    private final CarpoolPassengerJpaRepository jpaRepository;

    public CarpoolPassengerRepositoryAdapter(CarpoolPassengerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CarpoolPassenger save(CarpoolPassenger passenger) {
        CarpoolPassengerEntity entity = new CarpoolPassengerEntity();
        entity.setCarpoolId(passenger.getCarpoolId());
        entity.setPassengerId(passenger.getPassengerId());
        entity.setJoinedAt(passenger.getJoinedAt());
        entity.setLeftAt(passenger.getLeftAt());
        CarpoolPassengerEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public int countActive(Long carpoolId) {
        return jpaRepository.countByCarpoolIdAndLeftAtIsNull(carpoolId);
    }

    @Override
    public boolean existsActive(Long carpoolId, Long passengerId) {
        return jpaRepository.existsByCarpoolIdAndPassengerIdAndLeftAtIsNull(carpoolId, passengerId);
    }

    @Override
    public Optional<CarpoolPassenger> findActiveByPassengerIdAndCarpoolIds(Long passengerId, List<Long> carpoolIds) {
        if (carpoolIds == null || carpoolIds.isEmpty()) {
            return Optional.empty();
        }
        return jpaRepository.findActiveByPassengerIdAndCarpoolIds(passengerId, carpoolIds)
                .map(this::toDomain);
    }

    @Override
    public void removePassenger(Long carpoolId, Long passengerId) {
        jpaRepository.softDeleteByPassengerIdAndCarpoolId(carpoolId, passengerId, LocalDateTime.now());
    }

    @Override
    public void removeAllByCarpoolId(Long carpoolId) {
        jpaRepository.softDeleteAllByCarpoolId(carpoolId, LocalDateTime.now());
    }

    @Override
    public void removeAllByCarpoolIds(List<Long> carpoolIds) {
        if (carpoolIds == null || carpoolIds.isEmpty()) {
            return;
        }
        jpaRepository.softDeleteAllByCarpoolIds(carpoolIds, LocalDateTime.now());
    }

    private CarpoolPassenger toDomain(CarpoolPassengerEntity entity) {
        return CarpoolPassenger.builder()
                .id(entity.getId())
                .carpoolId(entity.getCarpoolId())
                .passengerId(entity.getPassengerId())
                .joinedAt(entity.getJoinedAt())
                .leftAt(entity.getLeftAt())
                .build();
    }
}
