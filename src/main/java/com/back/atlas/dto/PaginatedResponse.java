package com.back.atlas.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class PaginatedResponse {
    private int page;
    private int size;
    private int total;
    private int totalPages;
    private List<JsonElement> data;
    private boolean hasNext;
    private boolean hasPrevious;
}
