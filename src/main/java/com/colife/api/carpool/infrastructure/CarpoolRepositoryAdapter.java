package com.colife.api.carpool.infrastructure;

import org.springframework.stereotype.Component;

import com.colife.api.carpool.domain.Carpool;
import com.colife.api.carpool.domain.CarpoolMapper;
import com.colife.api.carpool.domain.CarpoolRepositoryPort;
import com.colife.api.carpool.domain.CarpoolStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CarpoolRepositoryAdapter implements CarpoolRepositoryPort {

    private final CarpoolJpaRepository carpoolJpaRepository;

    public CarpoolRepositoryAdapter(CarpoolJpaRepository carpoolJpaRepository) {
        this.carpoolJpaRepository = carpoolJpaRepository;
    }

    @Override
    public Carpool save(Carpool carpool) {
        CarpoolEntity entity = CarpoolMapper.toEntity(carpool);
        return CarpoolMapper.toDomain(carpoolJpaRepository.save(entity));
    }

    @Override
    public Optional<Carpool> findById(Long id) {
        return carpoolJpaRepository.findById(id).map(CarpoolMapper::toDomain);
    }

    @Override
    public Optional<Carpool> findByActivityId(Long activityId) {
        return carpoolJpaRepository.findFirstByActivityIdAndStatusOrderByIdAsc(activityId, CarpoolStatus.ACTIVE).map(CarpoolMapper::toDomain);
    }

    @Override
    public List<Carpool> findAllActiveByActivityId(Long activityId) {
        return carpoolJpaRepository.findByActivityIdAndStatus(activityId, CarpoolStatus.ACTIVE)
                .stream()
                .map(CarpoolMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Carpool> findActiveByDriverIdAndActivityId(UUID driverId, Long activityId) {
        return carpoolJpaRepository.findByDriverIdAndActivityIdAndStatus(driverId, activityId, CarpoolStatus.ACTIVE)
                .map(CarpoolMapper::toDomain);
    }

    @Override
    public void cancelByDriverIdAndActivityId(UUID driverId, Long activityId) {
        carpoolJpaRepository.cancelByDriverIdAndActivityId(driverId, activityId);
    }

    @Override
    public void cancelAllByActivityId(Long activityId) {
        carpoolJpaRepository.cancelAllByActivityId(activityId);
    }
}
