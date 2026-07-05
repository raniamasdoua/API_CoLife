package com.colife.api.user.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.colife.api.user.domain.Role;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    /**
     * Clé primaire = sub Keycloak (UUID). Assignée explicitement au provisioning JIT
     * (pas de @GeneratedValue : l'identité provient de l'IdP, pas de la base).
     */
    @Id
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(columnDefinition = "TEXT")
    private String bio;
    @Column(length = 30)
    private String phone;
    @Column(length = 255)
    private String address;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;

    public UserEntity(UUID id,
                      String firstName,
                      String lastName,
                      String email,
                      Role role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }
}
