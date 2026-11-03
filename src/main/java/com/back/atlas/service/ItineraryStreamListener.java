package com.back.atlas.service;

import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.entity.DayItinerary;

/** Events of a streamed generation, already in the frontend's format. */
public interface ItineraryStreamListener {
    void onDay(DayItinerary day);

    void onStatus(String message);

    void onResult(GeneratedItinerary itinerary);
}
