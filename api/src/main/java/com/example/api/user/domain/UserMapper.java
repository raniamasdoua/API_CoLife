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
                .build();
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole()
        );
    }
}
