package com.back.atlas.dto;

import com.back.atlas.entity.Accommodation;
import com.back.atlas.entity.DayItinerary;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * AI-generated itinerary as the frontend expects it (snake_case keys, costs as "€" strings).
 * The app serializes HTTP bodies with Gson (spring.http.converters.preferred-json-mapper),
 * hence @SerializedName.
 */
@Getter
@Builder
public class GeneratedItinerary {
    private String destination;

    @SerializedName("total_days")
    private int totalDays;

    @SerializedName("total_budget")
    private String totalBudget;

    private List<String> interests;

    private List<DayItinerary> itinerary;

    private Accommodation accommodation;

    private List<String> tips;

    @SerializedName("total_estimated_cost")
    private String totalEstimatedCost;

    @SerializedName("budget_status")
    private String budgetStatus;

    /** "llm" or "fallback": lets the UI tell the user when the plan is a generic template. */
    private String source;

    private String model;
}
