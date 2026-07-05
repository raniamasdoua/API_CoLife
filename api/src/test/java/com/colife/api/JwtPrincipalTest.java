package com.colife.api;

import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.user.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPrincipalTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void hasRole_returns_true_when_role_matches() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "admin@test.com", Role.ADMIN);
        assertThat(principal.hasRole(Role.ADMIN)).isTrue();
    }

    @Test
    void hasRole_returns_false_when_role_does_not_match() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "user@test.com", Role.COLLABORATOR);
        assertThat(principal.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void hasRole_returns_false_when_role_is_null() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "user@test.com", null);
        assertThat(principal.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void isAdmin_returns_true_for_admin_role() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "admin@test.com", Role.ADMIN);
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    void isAdmin_returns_false_for_collaborator_role() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "user@test.com", Role.COLLABORATOR);
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void record_accessors_return_constructor_values() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "test@test.com", Role.COLLABORATOR);
        assertThat(principal.userId()).isEqualTo(USER_ID);
        assertThat(principal.email()).isEqualTo("test@test.com");
        assertThat(principal.role()).isEqualTo(Role.COLLABORATOR);
    }
}
