package com.back.atlas.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DayItinerary implements Serializable {
    private int day;
    private ActivityDetail morning;
    private ActivityDetail afternoon;
    private ActivityDetail evening;
}
