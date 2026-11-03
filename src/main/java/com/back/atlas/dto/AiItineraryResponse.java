package com.back.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiItineraryResponse {
    private boolean success;
    private GeneratedItinerary itinerary;
}
