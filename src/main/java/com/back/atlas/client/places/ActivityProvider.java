package com.back.atlas.client.places;

import com.back.atlas.dto.Place;

import java.util.List;

/**
 * Source of real places to visit. The rest of the app depends on this interface, not on a
 * provider: Amadeus was replaced by OpenStreetMap without touching controllers or services.
 */
public interface ActivityProvider {

    /** Places within radiusMeters of a point, most notable first. */
    List<Place> nearby(double latitude, double longitude, int radiusMeters, int limit);

    /** The most notable places of a city, or an empty list when the city is not found. */
    List<Place> inCity(String city, int limit);

    /** The provider could not answer (network error, timeout, unexpected response). */
    class ProviderException extends RuntimeException {
        public ProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
