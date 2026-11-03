package com.back.atlas.controller;

import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.AiItineraryResponse;
import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.dto.UpdateItineraryRequest;
import com.back.atlas.entity.User;
import com.back.atlas.service.ItineraryGenerationService;
import com.back.atlas.service.ItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final ItineraryGenerationService generationService;

    /** Public endpoint: errors are handled by GlobalExceptionHandler (400, 502). */
    @PostMapping("/generate")
    public AiItineraryResponse generateItinerary(@Valid @RequestBody AiItineraryRequest request) {
        return new AiItineraryResponse(true, generationService.generate(request));
    }

    /**
     * The owner is always the authenticated user. The path variable is kept for compatibility
     * with the frontend and must match the token's user.
     */
    @PostMapping("/save/{userId}")
    public ItineraryResponse saveItinerary(@RequestBody SaveItineraryRequest request,
                                           @PathVariable Long userId,
                                           @AuthenticationPrincipal User currentUser) {
        if (!currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot save itineraries for another user");
        }
        return itineraryService.saveForCurrentUser(request);
    }

    @GetMapping("/user")
    public List<ItineraryResponse> getAllUserItineraries() {
        return itineraryService.getAllUserItineraries();
    }

    @GetMapping("/{id}")
    public ItineraryResponse getItineraryById(@PathVariable Long id) {
        return itineraryService.getItineraryById(id);
    }

    @PutMapping("/{id}")
    public ItineraryResponse updateItinerary(@PathVariable Long id,
                                             @Valid @RequestBody UpdateItineraryRequest request) {
        return itineraryService.updateItinerary(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public List<ItineraryResponse> searchItineraries(@RequestParam String destination) {
        return itineraryService.searchItineraries(destination);
    }

    @GetMapping("/count")
    public Map<String, Long> getItineraryCount() {
        return Map.of("count", itineraryService.getItineraryCount());
    }
}
