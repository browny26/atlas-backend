package com.back.atlas.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Body of POST /save: the frontend sends back the GeneratedItinerary it received,
 * so the JSON keys are snake_case (deserialized by Gson).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SaveItineraryRequest {

    private String destination;

    @SerializedName("total_budget")
    private String totalBudget;

    @SerializedName("total_days")
    private int totalDays;

    private AccommodationDTO accommodation;
    private List<DayItineraryDTO> itinerary;
    private List<String> tips;

    @SerializedName(value = "total_estimated_cost", alternate = "totalEstimatedCost")
    private String totalEstimatedCost;

    @SerializedName(value = "budget_status", alternate = "budgetStatus")
    private String budgetStatus;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccommodationDTO {
        private String name;
        private String cost;
        private String type;
        private String details;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayItineraryDTO {
        private int day;
        private ActivityDTO morning;
        private ActivityDTO afternoon;
        private ActivityDTO evening;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ActivityDTO {
            private String activity;
            private String cost;
            private String time;
        }
    }
}
