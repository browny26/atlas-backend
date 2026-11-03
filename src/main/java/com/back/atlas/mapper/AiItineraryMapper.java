package com.back.atlas.mapper;

import com.back.atlas.client.ai.AiRegenerateDayRequest;
import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.dto.SaveItineraryRequest.DayItineraryDTO.ActivityDTO;
import com.back.atlas.entity.Accommodation;
import com.back.atlas.entity.ActivityDetail;
import com.back.atlas.entity.DayItinerary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates between the AI service's typed contract (numeric costs) and the shape used by
 * the frontend and the database ("€16" strings): an anti-corruption layer, so the two sides
 * can evolve independently.
 */
public final class AiItineraryMapper {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern TIME = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private AiItineraryMapper() {
    }

    // ---------- AI service -> frontend ----------

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

    public static DayItinerary toDay(AiServiceResponse.Day day) {
        return new DayItinerary(day.day(), toActivity(day.morning()), toActivity(day.afternoon()),
                toActivity(day.evening()));
    }

    /** 16.0 -> "€16", 16.5 -> "€16.50". */
    static String euros(double amount) {
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        String formatted = value.stripTrailingZeros().scale() <= 0
                ? value.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
                : value.toPlainString();
        return "€" + formatted;
    }

    private static ActivityDetail toActivity(AiServiceResponse.Activity activity) {
        return new ActivityDetail(activity.activity(), euros(activity.costEur()), activity.time());
    }

    private static Accommodation toAccommodation(AiServiceResponse.Accommodation accommodation) {
        return new Accommodation(accommodation.name(), euros(accommodation.costPerNightEur()) + "/night",
                accommodation.type(), accommodation.details());
    }

    // ---------- frontend -> AI service (for day regeneration) ----------

    /** Rebuilds the typed plan from what the frontend shows, tolerating older saved formats. */
    public static AiRegenerateDayRequest.Plan toAiPlan(SaveItineraryRequest itinerary) {
        List<AiServiceResponse.Day> days = itinerary.getItinerary().stream()
                .map(d -> new AiServiceResponse.Day(d.getDay(),
                        toAiActivity(d.getMorning(), "09:00"),
                        toAiActivity(d.getAfternoon(), "14:00"),
                        toAiActivity(d.getEvening(), "20:00")))
                .toList();
        SaveItineraryRequest.AccommodationDTO acc = itinerary.getAccommodation();
        String name = acc != null && acc.getName() != null ? acc.getName() : "Accommodation";
        AiServiceResponse.Accommodation accommodation = new AiServiceResponse.Accommodation(
                name,
                acc != null && acc.getType() != null ? acc.getType() : "hotel",
                acc != null && acc.getDetails() != null ? acc.getDetails() : name,
                acc != null ? parseEuros(acc.getCost()) : 0);
        List<String> tips = itinerary.getTips() != null && !itinerary.getTips().isEmpty()
                ? itinerary.getTips()
                : List.of("Book the main attractions in advance");
        return new AiRegenerateDayRequest.Plan(days, accommodation, tips);
    }

    private static AiServiceResponse.Activity toAiActivity(ActivityDTO activity, String defaultTime) {
        if (activity == null || activity.getActivity() == null || activity.getActivity().isBlank()) {
            return new AiServiceResponse.Activity("Free time", defaultTime, 0);
        }
        String time = activity.getTime() != null && TIME.matcher(activity.getTime()).matches()
                ? activity.getTime()
                : defaultTime;
        return new AiServiceResponse.Activity(activity.getActivity(), time, parseEuros(activity.getCost()));
    }

    /** "€16" -> 16, "€110/night" -> 110, "€20-€40" -> 20 (first amount), invalid -> 0. */
    public static double parseEuros(String text) {
        Matcher matcher = NUMBER.matcher(Objects.requireNonNullElse(text, ""));
        return matcher.find() ? Double.parseDouble(matcher.group().replace(',', '.')) : 0;
    }
}
