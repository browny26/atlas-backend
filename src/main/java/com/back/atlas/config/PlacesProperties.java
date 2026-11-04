package com.back.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * OpenStreetMap services used for activities: Overpass (places near a point) and
 * Nominatim (city name to coordinates). Both are free and need no API key, but their
 * usage policies ask for an identifying User-Agent and moderate traffic (hence caching).
 *
 * @param overpassUrl  Overpass API base URL
 * @param nominatimUrl Nominatim base URL
 * @param userAgent    identifies the app to the OSM services
 * @param timeout      read timeout for one request
 */
@ConfigurationProperties(prefix = "places.osm")
public record PlacesProperties(
        @DefaultValue("https://overpass-api.de") String overpassUrl,
        @DefaultValue("https://nominatim.openstreetmap.org") String nominatimUrl,
        @DefaultValue("Atlas-Travel-Planner/1.0 (+https://github.com/browny26/atlas-backend)") String userAgent,
        @DefaultValue("10s") Duration timeout
) {
}
