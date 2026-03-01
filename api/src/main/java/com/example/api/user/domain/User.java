package com.example.api.user.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;
}
