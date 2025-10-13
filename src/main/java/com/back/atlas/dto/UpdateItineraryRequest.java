package com.back.atlas.dto;

import com.back.atlas.entity.Accommodation;
import com.back.atlas.entity.DayItinerary;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryRequest {
    private String destination;
    private String totalBudget;
    private Integer totalDays;
    private Accommodation accommodation;
    private List<DayItinerary> itinerary;
    private List<String> tips;
    private String totalEstimatedCost;
    private String budgetStatus;

}
