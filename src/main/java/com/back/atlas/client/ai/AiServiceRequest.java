package com.back.atlas.client.ai;

import java.util.List;

/** Request body of POST /v1/itineraries on the AI service (sent as snake_case JSON). */
public record AiServiceRequest(
        String destination,
        int days,
        double budgetEur,
        List<String> interests,
        List<KnownActivity> knownActivities
) {
    /** A real place (from OpenStreetMap) the model should prefer: grounding. priceEur is 0 for free entry, otherwise null. */
    public record KnownActivity(String name, Double priceEur) {
    }
}
