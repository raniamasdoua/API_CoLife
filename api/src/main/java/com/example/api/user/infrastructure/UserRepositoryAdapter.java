package com.example.api.user.infrastructure;

import com.example.api.user.domain.User;
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
        /*
        @todo : à implémenter : faire le mapping entre User et UserEntity, puis utiliser jpaRepository.findById(id) pour récupérer l'entité et la convertir en domaine User
         */
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        /*
        @todo : à implémenter : faire le mapping entre User et UserEntity, puis utiliser jpaRepository.findByEmail(email) pour récupérer l'entité et la convertir en domaine User
         */
        return Optional.empty();
    }
}

