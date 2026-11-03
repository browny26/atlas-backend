package com.back.atlas.client.ai;

import java.util.List;

/** Request body of POST /v1/itineraries/regenerate-day on the AI service. */
public record AiRegenerateDayRequest(
        AiServiceRequest trip,
        Plan plan,
        int day,
        String feedback
) {
    public record Plan(
            List<AiServiceResponse.Day> days,
            AiServiceResponse.Accommodation accommodation,
            List<String> tips
    ) {
    }
}
