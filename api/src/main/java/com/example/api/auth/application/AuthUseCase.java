package com.example.api.auth.application;

import com.example.api.auth.application.dto.LoginRequestDto;
import com.example.api.auth.application.dto.LoginResponseDto;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.shared.exception.ConflictException;
import com.example.api.shared.exception.UnauthorizedException;
import com.example.api.shared.security.JwtPrincipal;
import com.example.api.shared.security.JwtService;
import com.example.api.user.domain.Role;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final Logger log = LoggerFactory.getLogger(AuthUseCase.class);

    public AuthUseCase(UserRepositoryPort userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login échoué: utilisateur non trouvé pour l'email");
                    return new UnauthorizedException("Identifiants invalides");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login échoué: mot de passe invalide pour l'utilisateur");
            throw new UnauthorizedException("Identifiants invalides");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        log.info("User loggué avec succès: {}", normalizedEmail);

        return LoginResponseDto.of(token);
    }

    /**
     * Retourne les informations complètes de l'utilisateur connecté.
     * Effectue une requête DB pour récupérer bio, phone, address et createdAt.
     */
    public MeResponseDto getMe(JwtPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));
        return new MeResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getBio(),
                user.getPhone(),
                user.getAddress(),
                user.getCreatedAt()
        );
    }
}