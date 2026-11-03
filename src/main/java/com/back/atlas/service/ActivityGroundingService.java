package com.back.atlas.service;

import com.back.atlas.client.ai.AiServiceRequest.KnownActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fetches real activities for a destination, so the LLM can use real names and prices
 * instead of inventing them (grounding).
 * Best effort: generation must never fail or wait long because Amadeus is slow or down.
 */
@Slf4j
@Service
public class ActivityGroundingService {

    private final AmadeusService amadeusService;
    private final boolean enabled;
    private final int maxActivities;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ActivityGroundingService(AmadeusService amadeusService,
                                    @Value("${ai.grounding.enabled:true}") boolean enabled,
                                    @Value("${ai.grounding.max-activities:10}") int maxActivities,
                                    @Value("${ai.grounding.timeout:4s}") Duration timeout) {
        this.amadeusService = amadeusService;
        this.enabled = enabled;
        this.maxActivities = maxActivities;
        this.timeout = timeout;
    }

    public List<KnownActivity> findActivities(String destination) {
        if (!enabled) {
            return List.of();
        }
        // "Rome, Italy" -> "Rome": Amadeus searches by city name.
        String city = destination.split(",")[0].replaceAll("[^\\p{L} '-]", "").trim();
        if (city.isEmpty()) {
            return List.of();
        }
        try {
            return CompletableFuture
                    .supplyAsync(() -> search(city), executor)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.info("Amadeus activities for '{}' timed out after {}", city, timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.info("Amadeus activities for '{}' unavailable: {}", city, e.getMessage());
        }
        return List.of();
    }

    private List<KnownActivity> search(String city) {
        try {
            List<KnownActivity> activities = amadeusService.findActivities(city, maxActivities);
            log.info("Grounding '{}' with {} Amadeus activities", city, activities.size());
            return activities;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
