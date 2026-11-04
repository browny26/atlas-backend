package com.back.atlas.client.places;

import com.back.atlas.config.PlacesProperties;
import com.back.atlas.dto.Place;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Places from OpenStreetMap: free, no API key, worldwide.
 * <ul>
 *   <li>Overpass returns the tagged places around a point;</li>
 *   <li>Nominatim turns a city name into coordinates (for itinerary grounding).</li>
 * </ul>
 * OSM has no popularity score: places with a Wikipedia/Wikidata entry are ranked first,
 * as a good proxy for "notable". Results are cached to respect the services' usage policies.
 */
@Slf4j
@Component
public class OsmActivityProvider implements ActivityProvider {

    private static final int CITY_RADIUS_METERS = 6_000;
    private static final int MAX_ELEMENTS = 300;

    private final RestClient restClient;
    private final PlacesProperties properties;
    /** Nominatim allows one request per second per application (single backend instance). */
    private final RequestPacer nominatimPacer;

    private final Cache<String, List<Place>> nearbyCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(1_000)
            .build();
    private final Cache<String, Optional<double[]>> cityCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(7))
            .maximumSize(5_000)
            .build();

    public OsmActivityProvider(@Qualifier("placesRestClient") RestClient restClient, PlacesProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.nominatimPacer = new RequestPacer(properties.nominatimInterval());
    }

    @Override
    public List<Place> nearby(double latitude, double longitude, int radiusMeters, int limit) {
        // ~100 m precision: clicks a few metres apart share the same cached answer.
        String key = String.format(Locale.ROOT, "%.3f,%.3f,%d", latitude, longitude, radiusMeters);
        List<Place> places = nearbyCache.get(key,
                k -> search(nearbyQuery(latitude, longitude, radiusMeters, false), latitude, longitude));
        return places.subList(0, Math.min(limit, places.size()));
    }

    @Override
    public List<Place> inCity(String city, int limit) {
        Optional<double[]> center = cityCache.get(city.toLowerCase(Locale.ROOT), k -> geocode(city));
        if (center.isEmpty()) {
            return List.of();
        }
        double lat = center.get()[0];
        double lon = center.get()[1];
        String key = String.format(Locale.ROOT, "city:%.3f,%.3f", lat, lon);
        // Only notable places (with a Wikidata entry): a whole city has thousands of POIs.
        List<Place> places = nearbyCache.get(key,
                k -> search(nearbyQuery(lat, lon, CITY_RADIUS_METERS, true), lat, lon));
        return places.subList(0, Math.min(limit, places.size()));
    }

    /** Overpass QL: museums, attractions, monuments, notable parks and venues around a point. */
    static String nearbyQuery(double lat, double lon, int radiusMeters, boolean notableOnly) {
        String around = String.format(Locale.ROOT, "(around:%d,%.6f,%.6f)", radiusMeters, lat, lon);
        String notable = notableOnly ? "[wikidata]" : "";
        return "[out:json][timeout:8];("
                + "nwr" + around + "[tourism~\"^(attraction|museum|gallery|viewpoint|zoo|aquarium|theme_park)$\"][name]" + notable + ";"
                + "nwr" + around + "[historic~\"^(monument|castle|ruins|archaeological_site|fort|palace|city_gate)$\"][name]" + notable + ";"
                + "nwr" + around + "[leisure~\"^(park|garden|nature_reserve)$\"][name][wikidata];"
                + "nwr" + around + "[amenity~\"^(theatre|arts_centre|marketplace)$\"][name]" + notable + ";"
                // "out center": body (node coordinates) plus a centre for ways and relations.
                // ("out tags" would drop the coordinates of every node.)
                + ");out center " + MAX_ELEMENTS + ";";
    }

    private List<Place> search(String query, double lat, double lon) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(properties.overpassUrl() + "/api/interpreter?data={query}", query)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ProviderException("OpenStreetMap (Overpass) request failed: " + e.getMessage(), e);
        }
        if (body == null || !body.path("elements").isArray()) {
            throw new ProviderException("Unexpected Overpass response", null);
        }
        // Timeouts and memory errors come back as HTTP 200 with a "remark" and partial (or
        // no) elements: that is a failure, and it must not be cached as a valid answer.
        String remark = body.path("remark").asText("");
        if (remark.contains("runtime error")) {
            throw new ProviderException("Overpass error: " + remark, null);
        }
        List<Place> places = rank(toCandidates(body.path("elements"), lat, lon));
        log.info("OpenStreetMap: {} places around {},{}", places.size(), lat, lon);
        return places;
    }

    private Optional<double[]> geocode(String city) {
        JsonNode body;
        try {
            nominatimPacer.awaitTurn();
            body = restClient.get()
                    .uri(properties.nominatimUrl() + "/search?q={city}&format=jsonv2&limit=1&featureType=settlement", city)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ProviderException("OpenStreetMap (Nominatim) request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Geocoding interrupted", e);
        }
        if (body == null || !body.isArray() || body.isEmpty()) {
            log.info("OpenStreetMap: city '{}' not found", city);
            return Optional.empty();
        }
        JsonNode first = body.get(0);
        return Optional.of(new double[]{first.path("lat").asDouble(), first.path("lon").asDouble()});
    }

    /** A place plus what the ranking needs but the API does not return. */
    record Candidate(Place place, boolean notable) {
    }

    private static List<Candidate> toCandidates(JsonNode elements, double fromLat, double fromLon) {
        List<Candidate> places = new ArrayList<>();
        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");
            String name = text(tags, "name");
            // Nodes have lat/lon; ways and relations have a "center" (out center).
            JsonNode position = element.has("lat") ? element : element.path("center");
            if (name == null || !position.has("lat")) {
                continue;
            }
            double lat = position.path("lat").asDouble();
            double lon = position.path("lon").asDouble();
            Place place = Place.builder()
                    .id(element.path("type").asText() + "/" + element.path("id").asText())
                    .name(name)
                    .category(category(tags))
                    .latitude(lat)
                    .longitude(lon)
                    .distanceMeters((int) Math.round(distanceMeters(fromLat, fromLon, lat, lon)))
                    .free(free(text(tags, "fee")))
                    .imageUrl(imageUrl(tags))
                    .description(text(tags, "description"))
                    .openingHours(text(tags, "opening_hours"))
                    .website(Optional.ofNullable(text(tags, "website")).orElse(text(tags, "contact:website")))
                    .wikipediaUrl(wikipediaUrl(text(tags, "wikipedia")))
                    .mapsUrl(String.format(Locale.ROOT, "https://www.google.com/maps/search/?api=1&query=%.6f%%2C%.6f", lat, lon))
                    .build();
            places.add(new Candidate(place, tags.hasNonNull("wikidata") || tags.hasNonNull("wikipedia")));
        }
        return places;
    }

    /** Notable first (Wikipedia or Wikidata entry, then a picture), then the closest. No duplicates. */
    static List<Place> rank(List<Candidate> candidates) {
        Set<String> seen = new HashSet<>();
        return candidates.stream()
                .sorted(Comparator.comparingInt(OsmActivityProvider::score).reversed()
                        .thenComparing(candidate -> candidate.place().getDistanceMeters()))
                .map(Candidate::place)
                .filter(place -> seen.add(place.getName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static int score(Candidate candidate) {
        return (candidate.notable() ? 4 : 0) + (candidate.place().getImageUrl() != null ? 1 : 0);
    }

    private static String category(JsonNode tags) {
        for (String key : List.of("tourism", "historic", "leisure", "amenity")) {
            String value = text(tags, key);
            if (value != null) {
                String label = value.replace('_', ' ');
                return Character.toUpperCase(label.charAt(0)) + label.substring(1);
            }
        }
        return "Place";
    }

    private static Boolean free(String fee) {
        if (fee == null) {
            return null;
        }
        return switch (fee.toLowerCase(Locale.ROOT)) {
            case "no" -> true;
            case "yes" -> false;
            default -> null;
        };
    }

    private static String imageUrl(JsonNode tags) {
        String image = text(tags, "image");
        if (image != null && image.startsWith("https://")) {
            return image;
        }
        String commons = text(tags, "wikimedia_commons");
        if (commons != null && commons.startsWith("File:")) {
            return "https://commons.wikimedia.org/wiki/Special:FilePath/" + encode(commons.substring(5)) + "?width=640";
        }
        return null;
    }

    /** "it:Colosseo" -> https://it.wikipedia.org/wiki/Colosseo */
    static String wikipediaUrl(String tag) {
        if (tag == null) {
            return null;
        }
        int colon = tag.indexOf(':');
        if (colon < 2 || colon == tag.length() - 1) {
            return null;
        }
        String language = tag.substring(0, colon);
        if (!language.matches("[a-z-]{2,12}")) {
            return null;
        }
        return "https://" + language + ".wikipedia.org/wiki/" + encode(tag.substring(colon + 1));
    }

    private static String encode(String title) {
        return URLEncoder.encode(title.trim().replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String text(JsonNode tags, String key) {
        JsonNode value = tags.get(key);
        return value == null || value.asText().isBlank() ? null : value.asText().trim();
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * earthRadius * Math.asin(Math.sqrt(a));
    }
}
