package com.example.api.user.infrastructure;

import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserMapper;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public void updatePassword(Long id, String encodedPassword) {
        UserEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        entity.setPassword(encodedPassword);
        entity.setResetToken(null);
        entity.setResetTokenExpiry(null);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<User> findByResetToken(String resetToken) {
        return jpaRepository.findByResetToken(resetToken)
                .map(UserMapper::toDomain);
    }

    @Override
    public void updateResetToken(Long id, String resetToken, java.time.LocalDateTime resetTokenExpiry) {
        UserEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        entity.setResetToken(resetToken);
        entity.setResetTokenExpiry(resetTokenExpiry);
        jpaRepository.save(entity);
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
