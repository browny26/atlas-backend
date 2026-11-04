package com.back.atlas.controller;

import com.back.atlas.client.places.ActivityProvider;
import com.back.atlas.dto.Airport;
import com.back.atlas.dto.PageResponse;
import com.back.atlas.dto.Place;
import com.back.atlas.exception.ExternalServiceException;
import com.back.atlas.service.AirportDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Activities near a point (OpenStreetMap) and airport search (static dataset). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
public class PlacesController {

    private static final int MAX_RESULTS = 100;
    private static final int MAX_RADIUS_KM = 10;

    private final ActivityProvider activityProvider;
    private final AirportDirectory airportDirectory;

    /** Logged-in users only (dashboard). Paged in memory: OSM answers are small and cached. */
    @GetMapping("/activities")
    public PageResponse<Place> activities(@RequestParam double latitude,
                                          @RequestParam double longitude,
                                          @RequestParam(defaultValue = "1") int radius,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "12") int size) {
        // !(x <= max) also rejects NaN, which fails every comparison.
        if (!(Math.abs(latitude) <= 90) || !(Math.abs(longitude) <= 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }
        if (radius < 1 || radius > MAX_RADIUS_KM || page < 1 || size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "radius must be 1-" + MAX_RADIUS_KM + " km, page >= 1, size 1-50");
        }
        try {
            List<Place> places = activityProvider.nearby(latitude, longitude, radius * 1000, MAX_RESULTS);
            return PageResponse.of(places, page, size);
        } catch (ActivityProvider.ProviderException e) {
            throw new ExternalServiceException("Activities are temporarily unavailable, please try again", e);
        }
    }

    /** Public: used by the flight form on the home page. */
    @GetMapping("/airports")
    public List<Airport> airports(@RequestParam String keyword) {
        if (keyword.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword is too long");
        }
        return airportDirectory.search(keyword, 10);
    }
}
