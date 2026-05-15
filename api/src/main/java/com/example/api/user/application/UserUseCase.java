package com.example.api.user.application;

import com.example.api.shared.exception.ResourceNotFoundException;
import com.example.api.shared.exception.UnauthorizedException;
import com.example.api.user.application.dto.ChangePasswordRequestDto;
import com.example.api.user.application.dto.UpdateProfileRequestDto;
import com.example.api.user.application.dto.UserResponseDto;
import com.example.api.user.domain.User;
import com.example.api.user.domain.UserRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserUseCase(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Modifie le mot de passe d'un utilisateur après vérification du mot de passe actuel.
     * Accessible uniquement par l'utilisateur lui-même (le contrôleur s'en assure via @PreAuthorize).
     */
    public void changePassword(Long userId, ChangePasswordRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Mot de passe actuel incorrect");
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        userRepository.updatePassword(userId, encodedNewPassword);
    }

    /** Retourne la liste de tous les utilisateurs (admin uniquement). */
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserUseCase::toDto)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .collect(Collectors.toList());
    }

    /** Retourne le nombre total d'utilisateurs enregistrés. */
    public long countUsers() {
        return userRepository.countAll();
    }

    /** Retourne le profil complet d'un utilisateur. */
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return toDto(user);
    }

    /**
     * Met à jour les champs modifiables (bio, phone, address) du profil.
     * Les valeurs null dans la requête sont ignorées (patch partiel).
     */
    public UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        User updated = existing
                .withBio(request.bio() != null ? request.bio() : existing.getBio())
                .withPhone(request.phone() != null ? request.phone() : existing.getPhone())
                .withAddress(request.address() != null ? request.address() : existing.getAddress());

        User saved = userRepository.update(updated);
        return toDto(saved);
    }

    // ─── Mapping privé ────────────────────────────────────────────────────────

    private static UserResponseDto toDto(User u) {
        return new UserResponseDto(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getRole(),
                u.getBio(),
                u.getPhone(),
                u.getAddress(),
                u.getCreatedAt()
        );
    }
}
