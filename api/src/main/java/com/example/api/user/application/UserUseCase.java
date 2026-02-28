package com.example.api.user.application;

import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserUseCase {
    private final UserRepositoryPort userRepository;
    private static final Logger log = LoggerFactory.getLogger(UserUseCase.class);

    public UserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
