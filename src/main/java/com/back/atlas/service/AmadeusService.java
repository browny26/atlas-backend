package com.back.atlas.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.Response;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.*;
import com.back.atlas.client.ai.AiServiceRequest.KnownActivity;
import com.back.atlas.dto.PaginatedResponse;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AmadeusService {

    private final Amadeus amadeus;

    public AmadeusService(
            @Value("${amadeus.api.key}") String apiKey,
            @Value("${amadeus.api.secret}") String apiSecret
    ) {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    // Cerca aeroporti
    public Location[] location(String keyword) throws ResponseException {
        return amadeus.referenceData.locations.get(Params
                .with("keyword", keyword)
                .and("subType", "AIRPORT"));
    }

    public FlightOfferSearch[] flights(
            String origin,
            String destination,
            String departDate,
            String returnDate,
            int adults
    ) throws ResponseException {
        return amadeus.shopping.flightOffersSearch.get(
                Params.with("originLocationCode", origin)
                        .and("destinationLocationCode", destination)
                        .and("departureDate", departDate)
                        .and("returnDate", returnDate)
                        .and("adults", adults)
                        .and("max", 3)
        );
    }

    public JsonObject getHotelsByCity(String cityCode, int radius)
            throws ResponseException {

        Response response = amadeus.get(
                "/v1/reference-data/locations/hotels/by-city",
                Params.with("cityCode", cityCode)
                        .and("radius", radius)
                        .and("radiusUnit", "KM")
                        .and("hotelSource", "ALL")
        );

        return response.getResult();
    }

    public PaginatedResponse getToursAndActivities(double latitude, double longitude, int radius, int page, int size) throws ResponseException {
        Response response = amadeus.get("/v1/shopping/activities", Params.with("latitude", latitude).and("longitude", longitude).and("radius", radius));

        JsonObject allResults = response.getResult();

        JsonArray data = allResults.getAsJsonArray("data");
        int total = data.size();

        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<JsonElement> paged = IntStream.range(fromIndex, toIndex)
                .mapToObj(data::get)
                .collect(Collectors.toList());

        boolean hasNext = toIndex < total;
        boolean hasPrevious = fromIndex > 0;

        int totalPages = (int) Math.ceil((double) total / size);

        return new PaginatedResponse(page, size, total, totalPages, paged, hasNext, hasPrevious);
    }

    /**
     * Real activities near a city, used to ground AI itineraries.
     * Returns an empty list when the city is unknown to Amadeus.
     */
    public List<KnownActivity> findActivities(String city, int limit) throws ResponseException {
        Location[] cities = amadeus.referenceData.locations.get(Params
                .with("keyword", city)
                .and("subType", "CITY")
                .and("page[limit]", 1));
        if (cities.length == 0 || cities[0].getGeoCode() == null) {
            return List.of();
        }
        Activity[] activities = amadeus.shopping.activities.get(Params
                .with("latitude", cities[0].getGeoCode().getLatitude())
                .and("longitude", cities[0].getGeoCode().getLongitude())
                .and("radius", 10));
        return Arrays.stream(activities)
                .filter(a -> a.getName() != null && !a.getName().isBlank())
                .limit(limit)
                .map(a -> new KnownActivity(a.getName(), priceInEuros(a)))
                .toList();
    }

    /** Only EUR prices are passed on: converting currencies is out of scope here. */
    private static Double priceInEuros(Activity activity) {
        Activity.ElementaryPrice price = activity.getPrice();
        if (price == null || price.getAmount() == null || !"EUR".equals(price.getCurrencyCode())) {
            return null;
        }
        try {
            return Double.parseDouble(price.getAmount());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public JsonObject getAirportAndCityInfo(String keyword, String subType) throws ResponseException {
        Response response = amadeus.get("/v1/reference-data/locations", Params.with("keyword", keyword).and("subType", subType).and("page[limit]", 10));
        return response.getResult();
    }
}
