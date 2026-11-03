package com.back.atlas.mapper;

import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.entity.Accommodation;
import com.back.atlas.entity.ActivityDetail;
import com.back.atlas.entity.DayItinerary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Translates the AI service's typed contract into the shape used by the frontend and the
 * database (anti-corruption layer): the two sides can evolve independently.
 */
public final class AiItineraryMapper {

    private AiItineraryMapper() {
    }

    public static GeneratedItinerary toGeneratedItinerary(AiServiceResponse response, List<String> interests) {
        return GeneratedItinerary.builder()
                .destination(response.destination())
                .totalDays(response.totalDays())
                .totalBudget(euros(response.budgetEur()))
                .interests(interests)
                .itinerary(response.days().stream().map(AiItineraryMapper::toDay).toList())
                .accommodation(toAccommodation(response.accommodation()))
                .tips(response.tips())
                .totalEstimatedCost(euros(response.totalEstimatedCostEur()))
                .budgetStatus(response.budgetStatus())
                .source(response.metadata().source())
                .model(response.metadata().model())
                .build();
    }

    /** 16.0 -> "€16", 16.5 -> "€16.50". */
    static String euros(double amount) {
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        String formatted = value.stripTrailingZeros().scale() <= 0
                ? value.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
                : value.toPlainString();
        return "€" + formatted;
    }

    private static DayItinerary toDay(AiServiceResponse.Day day) {
        return new DayItinerary(day.day(), toActivity(day.morning()), toActivity(day.afternoon()),
                toActivity(day.evening()));
    }

    private static ActivityDetail toActivity(AiServiceResponse.Activity activity) {
        return new ActivityDetail(activity.activity(), euros(activity.costEur()), activity.time());
    }

    private static Accommodation toAccommodation(AiServiceResponse.Accommodation accommodation) {
        return new Accommodation(accommodation.name(), euros(accommodation.costPerNightEur()) + "/night",
                accommodation.type(), accommodation.details());
    }
}
