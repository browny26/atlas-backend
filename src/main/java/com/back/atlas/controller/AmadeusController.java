package com.back.atlas.controller;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.*;
import com.back.atlas.dto.PaginatedResponse;
import com.back.atlas.service.AmadeusService;
import com.google.gson.JsonObject;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/amadeus")
public class AmadeusController {

    private final AmadeusService amadeusService;

    public AmadeusController(AmadeusService amadeusService) {
        this.amadeusService = amadeusService;
    }

    @GetMapping("/locations")
    public Location[] locations(@RequestParam(required=true) String keyword) throws ResponseException {
        return amadeusService.location(keyword);
    }

    @GetMapping("/flights")
    public FlightOfferSearch[] flights(@RequestParam(required=true) String origin,
                                       @RequestParam(required=true) String destination,
                                       @RequestParam(required=true) String departDate,
                                       @RequestParam(required = false) String returnDate,
                                       @RequestParam(required=true) int adults)
            throws ResponseException {
        return amadeusService.flights(origin, destination, departDate,returnDate, adults);
    }

    @GetMapping("/hotels")
    public JsonObject getHotels(@RequestParam String cityCode, @RequestParam int radius) throws ResponseException {
        return amadeusService.getHotelsByCity(cityCode,radius);
    }

    @GetMapping("/tours-and-activities")
    public PaginatedResponse getToursAndActivities(@RequestParam double latitude, @RequestParam double longitude, @RequestParam(defaultValue = "1") int radius, @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) throws ResponseException {
        System.out.println("Latitude: " + latitude + ", Longitude: " + longitude + ", Radius: " + radius + ", Page: " + page + ", Size: " + size);
        return amadeusService.getToursAndActivities(latitude,longitude,radius, page, size);
    }

    @GetMapping("/airport-and-city-search")
    public JsonObject airportAndCitySearch(@RequestParam String keyword, @RequestParam String subType) throws ResponseException {
        return amadeusService.getAirportAndCityInfo(keyword, subType);
    }
}
