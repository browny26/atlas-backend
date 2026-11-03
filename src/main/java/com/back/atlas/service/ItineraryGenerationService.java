package com.back.atlas.service;

import com.back.atlas.client.ai.AiItineraryClient;
import com.back.atlas.client.ai.AiServiceRequest;
import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.mapper.AiItineraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Generates itineraries by delegating to the AI microservice. */
@Service
@RequiredArgsConstructor
public class ItineraryGenerationService {

    private final AiItineraryClient aiClient;

    public GeneratedItinerary generate(AiItineraryRequest request) {
        List<String> interests = request.getInterests() != null ? request.getInterests() : List.of();
        AiServiceRequest aiRequest = new AiServiceRequest(
                request.getDestination().trim(),
                request.getDays(),
                parseBudget(request.getBudget()),
                interests
        );

        AiServiceResponse response = aiClient.generateItinerary(aiRequest);
        return AiItineraryMapper.toGeneratedItinerary(response, interests);
    }

    /** Accepts "500", "500.50", "€500" or "500 €" (the frontend sends free text). */
    static double parseBudget(String budget) {
        String digits = budget.replaceAll("[^0-9.,]", "").replace(',', '.');
        try {
            double value = Double.parseDouble(digits);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // handled below
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget must be a positive amount in euros");
    }
}
