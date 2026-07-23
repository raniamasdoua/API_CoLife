package com.colife.api.user.application;

import org.springframework.stereotype.Service;

import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.user.application.dto.UpdateProfileRequestDto;
import com.colife.api.user.application.dto.UserResponseDto;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserUseCase {

    private final UserRepositoryPort userRepository;

    public UserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
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
    public UserResponseDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return toDto(user);
    }

    /**
     * Met à jour les champs modifiables (bio, phone, address) du profil.
     * Les valeurs null dans la requête sont ignorées (patch partiel).
     */
    public UserResponseDto updateProfile(UUID userId, UpdateProfileRequestDto request) {
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
