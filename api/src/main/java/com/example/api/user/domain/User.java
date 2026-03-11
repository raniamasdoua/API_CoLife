package com.example.api.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;

@Getter
@Builder
@With
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;
    private String bio;
    private String phone;
    private String address;
    private LocalDate createdAt;
}
