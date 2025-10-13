package com.back.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AiItineraryResponse {
    private boolean success;
    private Object itinerary;
}
