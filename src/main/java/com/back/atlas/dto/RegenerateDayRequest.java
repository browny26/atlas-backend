package com.back.atlas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Rewrite one day of the itinerary the frontend is showing (as returned by /generate). */
@Getter
@Setter
public class RegenerateDayRequest {
    @NotNull
    @Valid
    private SaveItineraryRequest itinerary;

    @Min(1)
    @Max(14)
    private int day;

    @Size(max = 300)
    private String feedback;
}
