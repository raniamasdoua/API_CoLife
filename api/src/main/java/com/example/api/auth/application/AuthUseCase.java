package com.example.api.auth.application;

import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.user.domain.Role;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import com.example.api.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Logger log = LoggerFactory.getLogger(AuthUseCase.class);

    public AuthUseCase(UserRepositoryPort userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequestDto request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email déjà utilisé");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(hashedPassword)
                .role(Role.COLLABORATOR)
                .build();

        log.info("Registering new user: {}", normalizedEmail);

        userRepository.save(user);
    }
}