package com.example.api.user.application;

import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserUseCase {
    private final UserRepositoryPort userRepository;

    public UserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
