package com.back.atlas.client.ai;

import java.util.List;

/** Response of the AI service: a typed, validated itinerary (received as snake_case JSON). */
public record AiServiceResponse(
        String destination,
        int totalDays,
        String currency,
        double budgetEur,
        int nights,
        double totalEstimatedCostEur,
        String budgetStatus,
        List<Day> days,
        Accommodation accommodation,
        List<String> tips,
        Metadata metadata
) {
    public record Day(int day, Activity morning, Activity afternoon, Activity evening) {
    }

    public record Activity(String activity, String time, double costEur) {
    }

    public record Accommodation(String name, String type, String details, double costPerNightEur) {
    }

    /** How the itinerary was produced: source is "llm" or "fallback". */
    public record Metadata(String source, String model, String promptVersion, int attempts, long latencyMs) {
    }
}
