package com.back.atlas.service;

import com.back.atlas.client.ai.AiItineraryClient;
import com.back.atlas.client.ai.AiRegenerateDayRequest;
import com.back.atlas.client.ai.AiServiceRequest;
import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.client.ai.AiStreamListener;
import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.dto.RegenerateDayRequest;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.mapper.AiItineraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/** Generates itineraries by delegating to the AI microservice, grounded with real places (OpenStreetMap). */
@Service
@RequiredArgsConstructor
public class ItineraryGenerationService {

    private final AiItineraryClient aiClient;
    private final ActivityGroundingService groundingService;

    public GeneratedItinerary generate(AiItineraryRequest request) {
        AiServiceRequest aiRequest = toAiRequest(request);
        AiServiceResponse response = aiClient.generateItinerary(aiRequest);
        return AiItineraryMapper.toGeneratedItinerary(response, aiRequest.interests());
    }

    /** Streams the generation: days are forwarded as soon as the AI service sends them. */
    public void stream(AiItineraryRequest request, ItineraryStreamListener listener) {
        AiServiceRequest aiRequest = toAiRequest(request);
        aiClient.streamItinerary(aiRequest, new AiStreamListener() {
            @Override
            public void onDay(AiServiceResponse.Day day) {
                listener.onDay(AiItineraryMapper.toDay(day));
            }

            @Override
            public void onStatus(String message) {
                listener.onStatus(message);
            }

            @Override
            public void onResult(AiServiceResponse result) {
                listener.onResult(AiItineraryMapper.toGeneratedItinerary(result, aiRequest.interests()));
            }
        });
    }

    public GeneratedItinerary regenerateDay(RegenerateDayRequest request) {
        SaveItineraryRequest itinerary = request.getItinerary();
        if (itinerary.getItinerary() == null || request.getDay() > itinerary.getItinerary().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Day does not exist in the itinerary");
        }
        if (itinerary.getItinerary().stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary days must not be null");
        }
        // Blank feedback means "no feedback" (the AI service rejects empty strings).
        String feedback = request.getFeedback() == null || request.getFeedback().isBlank()
                ? null
                : request.getFeedback().trim();
        List<String> interests = itinerary.getInterests() != null ? itinerary.getInterests() : List.of();
        AiServiceRequest trip = new AiServiceRequest(
                itinerary.getDestination(),
                itinerary.getItinerary().size(),
                parseBudget(itinerary.getTotalBudget()),
                interests,
                List.of()); // no grounding here: a single day must stay fast

        AiServiceResponse response = aiClient.regenerateDay(new AiRegenerateDayRequest(
                trip, AiItineraryMapper.toAiPlan(itinerary), request.getDay(), feedback));
        return AiItineraryMapper.toGeneratedItinerary(response, interests);
    }

    private AiServiceRequest toAiRequest(AiItineraryRequest request) {
        String destination = request.getDestination().trim();
        return new AiServiceRequest(
                destination,
                request.getDays(),
                parseBudget(request.getBudget()),
                request.getInterests() != null ? request.getInterests() : List.of(),
                groundingService.findActivities(destination));
    }

    /** Accepts "500", "500.50", "€500" or "500 €" (the frontend sends free text). */
    static double parseBudget(String budget) {
        String digits = budget == null ? "" : budget.replaceAll("[^0-9.,]", "").replace(',', '.');
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
