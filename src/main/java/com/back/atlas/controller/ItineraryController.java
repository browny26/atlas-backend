package com.back.atlas.controller;

import com.back.atlas.dto.*;
import com.back.atlas.entity.Itinerary;
import com.back.atlas.mapper.ItineraryMapper;
import com.back.atlas.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping("/generate")
    public AiItineraryResponse getItineraryIdeas(@RequestBody AiItineraryRequest req) {
        try {
            Object itinerary = itineraryService.generateItinerary(req);
            return new AiItineraryResponse(true, itinerary);
        } catch (Exception e) {
            e.printStackTrace();
            return new AiItineraryResponse(false, "Error generating itinerary");
        }
    }

    @PostMapping("/save/{userId}")
    public Itinerary saveItinerary(@RequestBody SaveItineraryRequest dto, @PathVariable Long userId) {
        Itinerary itinerary = ItineraryMapper.toEntity(dto);
        return itineraryService.saveItineraryForUser(itinerary, userId);
    }

    @GetMapping("/user")
    public ResponseEntity<List<ItineraryResponse>> getAllUserItineraries() {
        List<ItineraryResponse> itineraries = itineraryService.getAllUserItineraries();
        return ResponseEntity.ok(itineraries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItineraryResponse> getItineraryById(@PathVariable Long id) {
        ItineraryResponse itinerary = itineraryService.getItineraryById(id);
        return ResponseEntity.ok(itinerary);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItineraryResponse> updateItinerary(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItineraryRequest request) {
        ItineraryResponse updatedItinerary = itineraryService.updateItinerary(id, request);
        return ResponseEntity.ok(updatedItinerary);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItineraryResponse>> searchItineraries(
            @RequestParam String destination) {
        List<ItineraryResponse> itineraries = itineraryService.searchItineraries(destination);
        return ResponseEntity.ok(itineraries);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getItineraryCount() {
        long count = itineraryService.getItineraryCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
