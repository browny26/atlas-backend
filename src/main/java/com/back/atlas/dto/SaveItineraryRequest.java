package com.back.atlas.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SaveItineraryRequest {

    private String destination;
    private String total_budget;
    private int total_days;

    private AccommodationDTO accommodation;
    private List<DayItineraryDTO> itinerary;
    private List<String> tips;

    private String totalEstimatedCost;
    private String budgetStatus;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccommodationDTO {
        private String name;
        private String cost;
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
