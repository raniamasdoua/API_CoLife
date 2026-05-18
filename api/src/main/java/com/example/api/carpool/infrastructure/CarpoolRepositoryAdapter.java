package com.example.api.carpool.infrastructure;

import com.example.api.carpool.domain.Carpool;
import com.example.api.carpool.domain.CarpoolMapper;
import com.example.api.carpool.domain.CarpoolRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CarpoolRepositoryAdapter implements CarpoolRepositoryPort {

    private final CarpoolJpaRepository carpoolJpaRepository;

    public CarpoolRepositoryAdapter(CarpoolJpaRepository carpoolJpaRepository) {
        this.carpoolJpaRepository = carpoolJpaRepository;
    }

    @Override
    public Carpool save(Carpool carpool) {
        CarpoolEntity entity = CarpoolMapper.toEntity(carpool);
        CarpoolEntity saved = carpoolJpaRepository.save(entity);
        return CarpoolMapper.toDomain(saved);
    }

    @Override
    public Optional<Carpool> findByActivityId(Long activityId) {
        return carpoolJpaRepository.findByActivityId(activityId)
                .map(CarpoolMapper::toDomain);
    }
}
