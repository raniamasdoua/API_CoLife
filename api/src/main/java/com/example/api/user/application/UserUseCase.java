package com.example.api.user.application;

import com.example.api.user.application.dto.UserResponseDto;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import com.example.api.shared.exception.ResourceNotFoundException;
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

    public UserResponseDto getUserById(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return new UserResponseDto(targetUser.getId(), targetUser.getEmail(), targetUser.getRole());
    }
}
