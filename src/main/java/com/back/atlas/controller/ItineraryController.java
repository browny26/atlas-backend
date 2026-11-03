package com.back.atlas.controller;

import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.AiItineraryResponse;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.dto.RegenerateDayRequest;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.dto.UpdateItineraryRequest;
import com.back.atlas.entity.DayItinerary;
import com.back.atlas.entity.User;
import com.back.atlas.service.ItineraryGenerationService;
import com.back.atlas.service.ItineraryService;
import com.back.atlas.service.ItineraryStreamListener;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final ItineraryGenerationService generationService;

    private static final long STREAM_TIMEOUT_MS = 120_000;

    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /** Public endpoint: errors are handled by GlobalExceptionHandler (400, 429, 502). */
    @PostMapping("/generate")
    public AiItineraryResponse generateItinerary(@Valid @RequestBody AiItineraryRequest request) {
        return new AiItineraryResponse(true, generationService.generate(request));
    }

    /**
     * Same as /generate, as Server-Sent Events: "day" events while the model writes,
     * then "result" (the full itinerary, authoritative) or "error".
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamItinerary(@Valid @RequestBody AiItineraryRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streamExecutor.execute(() -> {
            try {
                generationService.stream(request, new ItineraryStreamListener() {
                    @Override
                    public void onDay(DayItinerary day) {
                        send(emitter, "day", day);
                    }

                    @Override
                    public void onStatus(String message) {
                        send(emitter, "status", Map.of("message", message));
                    }

                    @Override
                    public void onResult(GeneratedItinerary itinerary) {
                        send(emitter, "result", new AiItineraryResponse(true, itinerary));
                    }
                });
                emitter.complete();
            } catch (ResponseStatusException e) {
                send(emitter, "error", Map.of("message", Objects.requireNonNullElse(e.getReason(), "Invalid request")));
                emitter.complete();
            } catch (Exception e) {
                log.warn("Itinerary stream failed: {}", e.getMessage());
                send(emitter, "error", Map.of("message", "The itinerary generator is temporarily unavailable"));
                emitter.complete();
            }
        });
        return emitter;
    }

    /** Rewrites one day of the itinerary the user is looking at, following optional feedback. */
    @PostMapping("/regenerate-day")
    public AiItineraryResponse regenerateDay(@Valid @RequestBody RegenerateDayRequest request) {
        return new AiItineraryResponse(true, generationService.regenerateDay(request));
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            // The client went away: stop producing events.
            throw new UncheckedIOException(e);
        }
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
