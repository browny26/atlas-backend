package com.back.atlas.service;

import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.dto.UpdateItineraryRequest;
import com.back.atlas.entity.Itinerary;
import com.back.atlas.entity.User;
import com.back.atlas.repository.ItineraryRepository;
import com.back.atlas.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryService {
    @Value("${hf.space.url}")
    private String SPACE_URL;

    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${groq.api.key}")
    private String groqApiKey;

    public Object generateItinerary(AiItineraryRequest req) {
        String groqUrl = "https://api.groq.com/openai/v1/chat/completions";

        String destination = req.getDestination() != null ? req.getDestination() : "Rome, Italy";
        int days = req.getDays() != 0 ? req.getDays() : 3;
        String budget = req.getBudget() != null ? req.getBudget() : "€500";
        List<String> interests = req.getInterests() != null ? req.getInterests() : Arrays.asList("general tourism");
        String interestsStr = String.join(", ", interests);

        String prompt = createExactPrompt(destination, days, budget, interestsStr, interests);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + groqApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "llama-3.1-8b-instant");
        payload.put("messages", Arrays.asList(Map.of("role", "user", "content", prompt)));
        payload.put("max_tokens", 2500);
        payload.put("temperature", 0.3);
        payload.put("top_p", 0.9);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(groqUrl, entity, Map.class);
            return processGroqResponse(response.getBody());

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate itinerary");
            errorResponse.put("details", e.getMessage());
            return errorResponse;
        }
    }

    private String createExactPrompt(String destination, int days, String budget, String interestsStr, List<String> interests) {
        // Converti la lista interests in JSON string per il prompt
        String interestsJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            interestsJson = mapper.writeValueAsString(interests);
        } catch (Exception e) {
            interestsJson = "[\"general tourism\"]";
        }

        return String.format("""
            <s>[INST] Generate a travel itinerary in valid JSON. Only JSON, no other text.
            IMPORTANT:
            1. Use ONLY Euros (€) as currency, not Yen or other currencies
            2. NO comments (//) in JSON
            3. Follow EXACTLY the required structure
            DESTINATION: %s
            NUMBER OF DAYS: %d
            TOTAL BUDGET: %s
            INTERESTS: %s
            CRITICAL BUDGET CONSTRAINT: The total estimated cost MUST be CLOSE TO %s (use 85-100%% of the budget). 
            Break down costs realistically and use most of the available budget for better experiences.
            JSON STRUCTURE (follow EXACTLY):
            {
              "destination": "%s",
              "total_days": %d,
              "total_budget": "%s",
              "interests": %s,
              "itinerary": [
                {
                  "day": 1,
                  "morning": {"activity": "string", "time": "HH:MM", "cost": "string (only Euros €)"},
                  "afternoon": {"activity": "string", "time": "HH:MM", "cost": "string (only Euros €)"},
                  "evening": {"activity": "string", "time": "HH:MM", "cost": "string (only Euros €)"}
                }
              ],
              "accommodation": {"name": "string", "cost": "string (only Euros €)", "type": "Accommodation type", "details": "Accommodation details and location"},
              "tips": [
                "Practical tip 1",
                "Practical tip 2", 
                "Practical tip 3"
              ],
              "total_estimated_cost": "string (only Euros €)",
              "budget_status": "within_budget"
            }
            IMPORTANT:
            - Return ONLY JSON, no text before or after
            - Transportation: Use public transport, walking when possible
            - Use EUR (€) for all costs
            - Total estimated cost should be CLOSE TO %s (use 85-100%% of budget)
            - Be realistic with prices and timings
            - Adapt activities to interests: %s
            - Include premium experiences to use the available budget
            - Use most of the budget for better accommodation, dining, and activities
            - JSON must be valid and parsable
            - Calculate total_estimated_cost accurately
            - Set budget_status to "within_budget" or "over_budget" based on your calculation
            [/INST]""",
                destination, days, budget, interestsStr, budget,
                destination, days, budget, interestsJson, budget, interestsStr);
    }

    private Object processGroqResponse(Map<String, Object> groqResponse) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResponse.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                String jsonStr = extractJsonFromText(content);
                if (jsonStr != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> itinerary = mapper.readValue(jsonStr, Map.class);

                    itinerary.put("source", "groq_direct");
                    itinerary.put("model", "llama-3.1-8b-instant");

                    return itinerary;
                }

                return Map.of(
                        "raw_content", content,
                        "source", "groq_direct",
                        "note", "Response is not valid JSON"
                );
            }

            return groqResponse;

        } catch (Exception e) {
            System.out.println("❌ Error processing response: " + e.getMessage());
            return groqResponse;
        }
    }

    private String extractJsonFromText(String text) {
        try {
            // Cerca JSON nel testo (rimuovi eventuali markdown)
            String cleanText = text.replace("```json", "").replace("```", "").trim();
            Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(cleanText);

            if (matcher.find()) {
                String jsonStr = matcher.group();
                // Valida che sia JSON parsabile
                ObjectMapper mapper = new ObjectMapper();
                mapper.readTree(jsonStr);
                return jsonStr;
            }
        } catch (Exception e) {
            System.out.println("❌ JSON extraction failed: " + e.getMessage());
        }
        return null;
    }

    public Itinerary saveItineraryForUser(Itinerary itinerary, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        itinerary.setUserId(user.getId());

        return itineraryRepository.save(itinerary);
    }

    public List<ItineraryResponse> getAllUserItineraries() {
        User currentUser = getCurrentUser();
        List<Itinerary> itineraries = itineraryRepository.findByUserId(currentUser.getId());

        return itineraries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ItineraryResponse getItineraryById(Long id) {
        User currentUser = getCurrentUser();
        Itinerary itinerary = itineraryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found or you don't have permission to access it"
                ));

        return mapToResponse(itinerary);
    }

    public ItineraryResponse updateItinerary(Long id, UpdateItineraryRequest request) {
        User currentUser = getCurrentUser();
        Itinerary itinerary = itineraryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found or you don't have permission to update it"
                ));

        if (request.getDestination() != null) {
            itinerary.setDestination(request.getDestination());
        }
        if (request.getTotalBudget() != null) {
            itinerary.setTotalBudget(request.getTotalBudget());
        }
        if (request.getTotalDays() != null) {
            itinerary.setTotalDays(request.getTotalDays());
        }
        if (request.getAccommodation() != null) {
            itinerary.setAccommodation(request.getAccommodation());
        }
        if (request.getItinerary() != null) {
            itinerary.setItinerary(request.getItinerary());
        }
        if (request.getTips() != null) {
            itinerary.setTips(request.getTips());
        }

        Itinerary updatedItinerary = itineraryRepository.save(itinerary);
        return mapToResponse(updatedItinerary);
    }

    public void deleteItinerary(Long id) {
        User currentUser = getCurrentUser();
        Itinerary itinerary = itineraryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found or you don't have permission to delete it"
                ));

        itineraryRepository.delete(itinerary);
    }

    public List<ItineraryResponse> searchItineraries(String destination) {
        User currentUser = getCurrentUser();
        List<Itinerary> itineraries = itineraryRepository.findByDestinationContainingIgnoreCase(destination);

        return itineraries.stream()
                .filter(itinerary -> itinerary.getUserId().equals(currentUser.getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getItineraryCount() {
        User currentUser = getCurrentUser();
        return itineraryRepository.countByUserId(currentUser.getId());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ItineraryResponse mapToResponse(Itinerary itinerary) {
        return ItineraryResponse.builder()
                .id(itinerary.getId())
                .destination(itinerary.getDestination())
                .totalBudget(itinerary.getTotalBudget())
                .totalDays(itinerary.getTotalDays())
                .accommodation(itinerary.getAccommodation())
                .itinerary(itinerary.getItinerary())
                .tips(itinerary.getTips())
                .userId(itinerary.getUserId())
                .totalEstimatedCost(itinerary.getTotalEstimatedCost())
                .budgetStatus(itinerary.getBudgetStatus())
                .build();
    }
}
