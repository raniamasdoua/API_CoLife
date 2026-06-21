package com.example.api.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@With
public class User {
    /** Identifiant = sub Keycloak (UUID). Assigné au provisioning, pas généré. */
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String bio;
    private String phone;
    private String address;
    private LocalDate createdAt;
}
