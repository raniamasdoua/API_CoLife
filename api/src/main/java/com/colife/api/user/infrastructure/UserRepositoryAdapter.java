package com.colife.api.user.infrastructure;

import org.springframework.stereotype.Component;

import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.user.domain.Role;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserMapper;
import com.colife.api.user.domain.UserRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(UserMapper::toDomain);
    }

    /** Crée un nouvel utilisateur (inscription). */
    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toNewEntity(user);
        UserEntity savedEntity = jpaRepository.save(entity);
        return UserMapper.toDomain(savedEntity);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    /**
     * Met à jour uniquement les champs modifiables (bio, phone, address)
     * d'un utilisateur existant.
     */
    @Override
    public User update(User user) {
        UserEntity entity = jpaRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        UserMapper.updateEntity(entity, user);
        UserEntity saved = jpaRepository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public void updateRole(UUID id, Role role) {
        jpaRepository.findById(id).ifPresent(entity -> {
            if (entity.getRole() != role) {
                entity.setRole(role);
                jpaRepository.save(entity);
            }
        });
    }
}
