package com.back.atlas.dto;

import com.back.atlas.entity.Accommodation;
import com.back.atlas.entity.DayItinerary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryResponse {
    private Long id;
    private String destination;
    private String totalBudget;
    private int totalDays;
    private Accommodation accommodation;
    private List<DayItinerary> itinerary;
    private String transportation;
    private List<String> tips;
    private Long userId;
    private String totalEstimatedCost;
    private String budgetStatus;
}
