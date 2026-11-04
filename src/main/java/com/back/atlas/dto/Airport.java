package com.back.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** An airport with scheduled flights (OurAirports data). Plain class: Gson serialization. */
@Getter
@AllArgsConstructor
public class Airport {
    private final String iataCode;
    private final String name;
    private final String cityName;
    private final String countryName;
    private final String countryCode;
    private final double latitude;
    private final double longitude;
}
