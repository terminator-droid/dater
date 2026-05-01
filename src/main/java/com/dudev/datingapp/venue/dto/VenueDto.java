package com.dudev.datingapp.venue.dto;

import com.dudev.datingapp.venue.entity.VenueCategory;

import java.util.UUID;

public record VenueDto(
        UUID id,
        String name,
        String address,
        double latitude,
        double longitude,
        String area,
        VenueCategory category
) {
}
