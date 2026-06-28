package com.example.api.activity.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Location {
    private LocationType locationType;
    private String room;
    private String street;
    private String complement;
    private String postalCode;
    private String city;
}
