package com.back.atlas.service;

import com.back.atlas.client.ai.AiServiceRequest.KnownActivity;
import com.back.atlas.client.places.ActivityProvider;
import com.back.atlas.dto.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fetches real, notable places of the destination (OpenStreetMap), so the LLM plans with
 * places that exist instead of inventing them (grounding).
 * Best effort: generation must never fail or wait long because the provider is slow or down.
 */
@Slf4j
@Service
public class ActivityGroundingService {

    private static final int MAX_IN_FLIGHT = 10;

    private final ActivityProvider activityProvider;
    private final boolean enabled;
    private final int maxActivities;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore inFlight = new Semaphore(MAX_IN_FLIGHT);

    public ActivityGroundingService(ActivityProvider activityProvider,
                                    @Value("${ai.grounding.enabled:true}") boolean enabled,
                                    @Value("${ai.grounding.max-activities:10}") int maxActivities,
                                    @Value("${ai.grounding.timeout:4s}") Duration timeout) {
        this.activityProvider = activityProvider;
        this.enabled = enabled;
        this.maxActivities = maxActivities;
        this.timeout = timeout;
    }

    public List<KnownActivity> findActivities(String destination) {
        if (!enabled) {
            return List.of();
        }
        // "Rome, Italy" -> "Rome": the provider searches by city name.
        String city = destination.split(",")[0].replaceAll("[^\\p{L} '-]", "").trim();
        if (city.isEmpty()) {
            return List.of();
        }
        // A slow provider could pile up lookups (one geocoding + one Overpass query each):
        // bound them, and skip grounding when too many are already in flight.
        if (!inFlight.tryAcquire()) {
            log.info("Too many activity lookups in flight, generating '{}' without grounding", city);
            return List.of();
        }
        // The permit is released exactly once: by the task when it ends, or below if the
        // task was cancelled before it started.
        AtomicBoolean started = new AtomicBoolean();
        AtomicBoolean released = new AtomicBoolean();
        Runnable releasePermit = () -> {
            if (released.compareAndSet(false, true)) {
                inFlight.release();
            }
        };
        Future<List<KnownActivity>> lookup;
        try {
            lookup = executor.submit(() -> {
                started.set(true);
                try {
                    return search(city);
                } finally {
                    releasePermit.run();
                }
            });
        } catch (RuntimeException e) {
            releasePermit.run();
            return List.of();
        }
        try {
            return lookup.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.info("Activities for '{}' timed out after {}", city, timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.info("Activities for '{}' unavailable: {}", city, e.getMessage());
        } finally {
            lookup.cancel(true); // no-op if finished; interrupts the virtual thread otherwise
            if (!started.get()) {
                releasePermit.run();
            }
        }
        return List.of();
    }

    private List<KnownActivity> search(String city) {
        if (Thread.currentThread().isInterrupted()) {
            return List.of();
        }
        List<KnownActivity> activities = activityProvider.inCity(city, maxActivities).stream()
                .map(ActivityGroundingService::toKnownActivity)
                .toList();
        log.info("Grounding '{}' with {} OpenStreetMap places", city, activities.size());
        return activities;
    }

    /** OSM has no prices; free entry (fee=no) is still useful to the model: price 0. */
    private static KnownActivity toKnownActivity(Place place) {
        String name = place.getName().length() > 150 ? place.getName().substring(0, 150) : place.getName();
        return new KnownActivity(name, Boolean.TRUE.equals(place.getFree()) ? 0.0 : null);
    }
}
