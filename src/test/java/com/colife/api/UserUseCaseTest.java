package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colife.api.shared.exception.ResourceNotFoundException;
import com.colife.api.user.application.UserUseCase;
import com.colife.api.user.application.dto.UpdateProfileRequestDto;
import com.colife.api.user.application.dto.UserResponseDto;
import com.colife.api.user.domain.Role;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private UserUseCase userUseCase;

    private static final LocalDate CREATED_AT = LocalDate.of(2024, Month.OCTOBER, 1);
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MISSING_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    // ─── getUserById ──────────────────────────────────────────────────────────

    @Test
    void should_return_full_profile_when_user_found() {
        // GIVEN
        User user = User.builder()
                .id(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@entreprise.com")
                .role(Role.COLLABORATOR)
                .bio("Passionnée de sport")
                .phone("+33600000000")
                .address("Paris, France")
                .createdAt(CREATED_AT)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // WHEN
        UserResponseDto result = userUseCase.getUserById(USER_ID);

        // THEN
        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.email()).isEqualTo("alice@entreprise.com");
        assertThat(result.role()).isEqualTo(Role.COLLABORATOR);
        assertThat(result.bio()).isEqualTo("Passionnée de sport");
        assertThat(result.phone()).isEqualTo("+33600000000");
        assertThat(result.address()).isEqualTo("Paris, France");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void should_throw_not_found_when_user_id_does_not_exist() {
        // GIVEN
        when(userRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> userUseCase.getUserById(MISSING_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utilisateur non trouvé");
    }

    // ─── updateProfile ────────────────────────────────────────────────────────

    @Test
    void should_update_bio_phone_address_and_return_updated_profile() {
        // GIVEN
        User existing = User.builder()
                .id(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@entreprise.com")
                .role(Role.COLLABORATOR)
                .bio("Ancienne bio")
                .phone("+33600000000")
                .address("Lyon, France")
                .createdAt(CREATED_AT)
                .build();

        User savedUser = existing
                .withBio("Nouvelle bio")
                .withPhone("+33611111111")
                .withAddress("Paris, France");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.update(any())).thenReturn(savedUser);

        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Nouvelle bio", "+33611111111", "Paris, France");

        // WHEN
        UserResponseDto result = userUseCase.updateProfile(USER_ID, request);

        // THEN
        assertThat(result.bio()).isEqualTo("Nouvelle bio");
        assertThat(result.phone()).isEqualTo("+33611111111");
        assertThat(result.address()).isEqualTo("Paris, France");
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.email()).isEqualTo("alice@entreprise.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(captor.capture());
        assertThat(captor.getValue().getBio()).isEqualTo("Nouvelle bio");
    }

    @Test
    void should_keep_existing_values_when_patch_fields_are_null() {
        // GIVEN
        User existing = User.builder()
                .id(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@entreprise.com")
                .role(Role.COLLABORATOR)
                .bio("Bio existante")
                .phone("+33600000000")
                .address("Paris, France")
                .createdAt(CREATED_AT)
                .build();

        User savedUser = existing.withPhone("+33699999999");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.update(any())).thenReturn(savedUser);

        // Seul le phone est modifié — bio et address sont null (patch partiel)
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                null, "+33699999999", null);

        // WHEN
        userUseCase.updateProfile(USER_ID, request);

        // THEN — les champs null gardent leurs valeurs
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(captor.capture());
        User captured = captor.getValue();
        assertThat(captured.getBio()).isEqualTo("Bio existante");
        assertThat(captured.getPhone()).isEqualTo("+33699999999");
        assertThat(captured.getAddress()).isEqualTo("Paris, France");
    }

    @Test
    void should_throw_not_found_when_updating_non_existing_user() {
        // GIVEN
        when(userRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        // WHEN / THEN
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("bio", null, null);
        assertThatThrownBy(() -> userUseCase.updateProfile(MISSING_ID, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utilisateur non trouvé");

        verify(userRepository, never()).update(any());
    }

    // ─── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returns_list_sorted_newest_first() {
        User older = User.builder().id(UUID.randomUUID()).firstName("Alice").lastName("A")
                .email("a@test.com").role(Role.COLLABORATOR).createdAt(LocalDate.of(2024, Month.JANUARY, 1)).build();
        User newer = User.builder().id(UUID.randomUUID()).firstName("Bob").lastName("B")
                .email("b@test.com").role(Role.COLLABORATOR).createdAt(LocalDate.of(2024, Month.JUNE, 1)).build();
        when(userRepository.findAll()).thenReturn(List.of(older, newer));

        List<UserResponseDto> result = userUseCase.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("Bob");
        assertThat(result.get(1).firstName()).isEqualTo("Alice");
    }

    // ─── countUsers ───────────────────────────────────────────────────────────

    @Test
    void countUsers_delegates_to_repository() {
        when(userRepository.countAll()).thenReturn(42L);

        assertThat(userUseCase.countUsers()).isEqualTo(42L);
    }

    // ─── getByEmail ───────────────────────────────────────────────────────────

    @Test
    void getByEmail_returns_present_optional_when_user_found() {
        User user = User.builder().id(USER_ID).firstName("Alice").lastName("Smith")
                .email("alice@test.com").role(Role.COLLABORATOR).createdAt(CREATED_AT).build();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        Optional<User> result = userUseCase.getByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
    }
}
