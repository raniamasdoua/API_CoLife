package com.example.api.user.domain;

import com.example.api.user.infrastructure.UserEntity;

public class UserMapper {

    public static User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .bio(entity.getBio())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Crée une nouvelle UserEntity à partir du domaine (utilisé uniquement
     * lors de l'inscription — l'id est null, createdAt sera généré par Hibernate).
     */
    public static UserEntity toNewEntity(User user) {
        UserEntity entity = new UserEntity(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole()
        );
        entity.setBio(user.getBio());
        entity.setPhone(user.getPhone());
        entity.setAddress(user.getAddress());
        return entity;
    }

    /**
     * Met à jour une UserEntity existante depuis le domaine
     * (utilisé pour PATCH profile : préserve l'id, createdAt et les champs non modifiables).
     */
    public static void updateEntity(UserEntity entity, User user) {
        entity.setBio(user.getBio());
        entity.setPhone(user.getPhone());
        entity.setAddress(user.getAddress());
    }
}
