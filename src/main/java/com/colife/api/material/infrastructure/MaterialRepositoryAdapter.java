package com.colife.api.material.infrastructure;

import org.springframework.stereotype.Component;

import com.colife.api.material.domain.MaterialMapper;
import com.colife.api.material.domain.MaterialProposal;
import com.colife.api.material.domain.MaterialRepositoryPort;

import java.util.List;
import java.util.Optional;

@Component
public class MaterialRepositoryAdapter implements MaterialRepositoryPort {

    private final MaterialJpaRepository materialJpaRepository;

    public MaterialRepositoryAdapter(MaterialJpaRepository materialJpaRepository) {
        this.materialJpaRepository = materialJpaRepository;
    }

    @Override
    public MaterialProposal save(MaterialProposal proposal) {
        MaterialEntity entity = MaterialMapper.toEntity(proposal);
        return MaterialMapper.toDomain(materialJpaRepository.save(entity));
    }

    @Override
    public Optional<MaterialProposal> findById(Long id) {
        return materialJpaRepository.findById(id).map(MaterialMapper::toDomain);
    }

    @Override
    public List<MaterialProposal> findAllByActivityId(Long activityId) {
        return materialJpaRepository.findByActivityIdOrderByCreatedAtAsc(activityId).stream()
                .map(MaterialMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        materialJpaRepository.deleteById(id);
    }
}
