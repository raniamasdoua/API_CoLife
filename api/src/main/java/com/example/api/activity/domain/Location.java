package com.example.api.activity.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Location {
    private String street;
    private String complement;
    private String postalCode;
    private String city;
}
