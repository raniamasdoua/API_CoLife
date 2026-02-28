package com.example.api.activity.infrastructure;

import jakarta.persistence.Embeddable;

@Embeddable
public class LocationEmbeddable {
    private String street;
    private String complement;
    private String postalCode;
    private String city;
}
