package com.example.api.user.infrastructure;

import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserMapper;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(Long id) {
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
}
