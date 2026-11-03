package com.back.atlas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiItineraryRequest {
    @NotBlank
    @Size(max = 100)
    private String destination;

    @Min(1)
    @Max(14)
    private int days;

    @NotBlank
    private String budget;

    @Size(max = 10)
    private List<String> interests;
}
