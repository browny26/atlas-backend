package com.back.atlas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiItineraryRequest {
    private String destination;
    private int days;
    private String budget;
    private List<String> interests;
}
