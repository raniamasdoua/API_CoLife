package com.colife.api.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.colife.api.user.domain.Role;
import com.colife.api.user.domain.User;
import com.colife.api.user.domain.UserRepositoryPort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Convertit un JWT Keycloak en authentification Spring.
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    private final UserRepositoryPort userRepository;

    public KeycloakJwtConverter(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");

        Set<String> realmRoles = extractRealmRoles(jwt);
        Role role = realmRoles.contains(Role.ADMIN.name()) ? Role.ADMIN : Role.COLLABORATOR;

        Collection<GrantedAuthority> authorities = realmRoles.stream()
                .filter(r -> r.equals(Role.ADMIN.name()) || r.equals(Role.COLLABORATOR.name()))
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        provisionOrSyncRole(userId, email, jwt, role);

        JwtPrincipal principal = new JwtPrincipal(userId, email, role);
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    private Set<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map<?, ?> map && map.get(ROLES_CLAIM) instanceof List<?> roles) {
            return roles.stream().map(Object::toString).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * Synchronise le miroir de profil local avec Keycloak :
     * - 1ʳᵉ connexion → crée la ligne {@code users} (provisioning JIT) ;
     * - connexions suivantes → met à jour le rôle s'il a changé côté Keycloak
     *   (ex. promotion en ADMIN), pour que {@code getMe()} reste cohérent avec le token.
     */
    private void provisionOrSyncRole(UUID userId, String email, Jwt jwt, Role role) {
        Optional<User> existing = userRepository.findById(userId);
        if (existing.isEmpty()) {
            User user = User.builder()
                    .id(userId)
                    .email(email)
                    .firstName(claimOrDefault(jwt, "given_name", ""))
                    .lastName(claimOrDefault(jwt, "family_name", ""))
                    .role(role)
                    .build();
            userRepository.save(user);
            return;
        }
        if (existing.get().getRole() != role) {
            userRepository.updateRole(userId, role);
        }
    }

    private String claimOrDefault(Jwt jwt, String claim, String fallback) {
        String value = jwt.getClaimAsString(claim);
        return value != null ? value : fallback;
    }
}
