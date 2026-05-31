package com.example.api.auth.application;

import com.example.api.auth.application.dto.ForgotPasswordRequestDto;
import com.example.api.auth.application.dto.ForgotPasswordResponseDto;
import com.example.api.auth.application.dto.LoginRequestDto;
import com.example.api.auth.application.dto.LoginResponseDto;
import com.example.api.auth.application.dto.MeResponseDto;
import com.example.api.auth.application.dto.RegisterRequestDto;
import com.example.api.auth.application.dto.ResetPasswordRequestDto;
import com.example.api.shared.exception.BusinessException;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final Logger log = LoggerFactory.getLogger(AuthUseCase.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();

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
                .bio(request.bio())
                .phone(request.phone())
                .address(request.address())
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

    public ForgotPasswordResponseDto forgotPassword(ForgotPasswordRequestDto request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        return userRepository.findByEmail(normalizedEmail)
                .map(user -> {
                    String token = generateResetToken();
                    LocalDateTime expiry = LocalDateTime.now().plusHours(1);
                    userRepository.updateResetToken(user.getId(), token, expiry);
                    log.info("Reset password token generated for user: {}", normalizedEmail);
                    return new ForgotPasswordResponseDto(
                            "Si un compte existe pour cet email, un lien de réinitialisation a été envoyé.",
                            token
                    );
                })
                .orElseGet(() -> new ForgotPasswordResponseDto(
                        "Si un compte existe pour cet email, un lien de réinitialisation a été envoyé.",
                        null
                ));
    }

    public void resetPassword(ResetPasswordRequestDto request) {
        if (request.token() == null || request.token().isBlank()) {
            throw new BusinessException("Le jeton de réinitialisation est invalide.");
        }

        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new BusinessException("Lien de réinitialisation invalide ou expiré."));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Lien de réinitialisation invalide ou expiré.");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        userRepository.updatePassword(user.getId(), encodedPassword);
    }

    private String generateResetToken() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return base64UrlEncoder.encodeToString(random);
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