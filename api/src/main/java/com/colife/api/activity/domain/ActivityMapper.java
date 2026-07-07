package com.colife.api.activity.domain;

import com.colife.api.activity.infrastructure.ActivityEntity;
import com.colife.api.activity.infrastructure.LocationEmbeddable;

public final class ActivityMapper {

    private ActivityMapper() {
    }

    public static Activity toDomain(ActivityEntity entity) {
        LocationEmbeddable loc = entity.getLocation();
        Location location = Location.builder()
                .locationType(loc.getLocationType())
                .room(loc.getRoom())
                .street(loc.getStreet())
                .complement(loc.getComplement())
                .postalCode(loc.getPostalCode())
                .city(loc.getCity())
                .build();

        return Activity.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .capacity(entity.getCapacity())
                .location(location)
                .typeId(entity.getType() != null ? entity.getType().getId() : null)
                .organizerId(entity.getOrganizer() != null ? entity.getOrganizer().getId() : null)
                .date(entity.getDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .deleted(entity.isDeleted())
                .locationType(loc.getLocationType())
                .build();
    }

    public static LocationEmbeddable toEmbeddable(Location location) {
        LocationEmbeddable emb = new LocationEmbeddable();
        emb.setLocationType(location.getLocationType());
        emb.setRoom(location.getRoom());
        emb.setStreet(location.getStreet());
        emb.setComplement(location.getComplement());
        emb.setPostalCode(location.getPostalCode());
        emb.setCity(location.getCity());
        return emb;
    }
}
