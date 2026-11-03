package com.back.atlas.client.ai;

import java.util.List;

/** Request body of POST /v1/itineraries on the AI service (sent as snake_case JSON). */
public record AiServiceRequest(
        String destination,
        int days,
        double budgetEur,
        List<String> interests
) {
}
