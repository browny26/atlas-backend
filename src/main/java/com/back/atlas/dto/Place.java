package com.back.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * A real place to visit (museum, monument, park, ...), from OpenStreetMap.
 * A plain class, not a record: the app serializes HTTP bodies with Gson.
 * Optional fields are null when OpenStreetMap has no data for them.
 */
@Getter
@Builder
@AllArgsConstructor
public class Place {
    private final String id;
    private final String name;
    private final String category;
    private final double latitude;
    private final double longitude;
    private final Integer distanceMeters;
    /** True when entry is free (OSM fee=no), false when paid, null when unknown. */
    private final Boolean free;
    private final String imageUrl;
    private final String description;
    private final String openingHours;
    private final String website;
    private final String wikipediaUrl;
    private final String mapsUrl;
}
