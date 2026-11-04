package com.back.atlas.service;

import com.back.atlas.client.ai.AiServiceRequest.KnownActivity;
import com.back.atlas.client.places.ActivityProvider;
import com.back.atlas.dto.Place;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ActivityGroundingServiceTest {

    private final ActivityProvider places = mock(ActivityProvider.class);

    private ActivityGroundingService service(Duration timeout) {
        return new ActivityGroundingService(places, true, 10, timeout);
    }

    private static Place place(String name, Boolean free) {
        return Place.builder().name(name).category("Museum").free(free).build();
    }

    @Test
    void searchesByCityNameAndMarksFreeEntry() {
        when(places.inCity("Paris", 10)).thenReturn(List.of(place("Louvre", false), place("Sacré-Cœur", true)));

        assertThat(service(Duration.ofSeconds(2)).findActivities("Paris, France")).containsExactly(
                new KnownActivity("Louvre", null),       // OSM has no prices
                new KnownActivity("Sacré-Cœur", 0.0));   // fee=no: free entry
    }

    @Test
    void returnsEmptyListWhenTheProviderFails() {
        when(places.inCity(anyString(), anyInt())).thenThrow(new ActivityProvider.ProviderException("down", null));

        assertThat(service(Duration.ofSeconds(2)).findActivities("Paris")).isEmpty();
    }

    @Test
    void doesNotWaitLongerThanTheTimeout() throws Exception {
        when(places.inCity(anyString(), anyInt())).thenAnswer(invocation -> {
            Thread.sleep(2_000);
            return List.of();
        });

        long start = System.nanoTime();
        List<KnownActivity> result = service(Duration.ofMillis(200)).findActivities("Paris");

        assertThat(result).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void timedOutLookupsAreCancelledAndDoNotLeakCapacity() throws Exception {
        when(places.inCity(anyString(), anyInt())).thenAnswer(invocation -> {
            Thread.sleep(5_000); // interrupted by the cancellation
            return List.of();
        });
        ActivityGroundingService service = service(Duration.ofMillis(30));

        // More calls than the in-flight limit (10): if cancelled lookups kept their permit,
        // the later calls would skip the provider entirely.
        for (int i = 0; i < 15; i++) {
            service.findActivities("Paris");
            Thread.sleep(20); // let the interrupted task finish
        }

        verify(places, times(15)).inCity("Paris", 10);
    }

    @Test
    void disabledMeansNoCall() {
        new ActivityGroundingService(places, false, 10, Duration.ofSeconds(1)).findActivities("Paris");

        verifyNoInteractions(places);
    }
}
