package com.back.atlas.service;

import com.amadeus.exceptions.ResponseException;
import com.back.atlas.client.ai.AiServiceRequest.KnownActivity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ActivityGroundingServiceTest {

    private final AmadeusService amadeus = mock(AmadeusService.class);

    private ActivityGroundingService service(Duration timeout) {
        return new ActivityGroundingService(amadeus, true, 10, timeout);
    }

    @Test
    void searchesByCityName() throws Exception {
        List<KnownActivity> activities = List.of(new KnownActivity("Louvre tour", 50.0));
        when(amadeus.findActivities("Paris", 10)).thenReturn(activities);

        assertThat(service(Duration.ofSeconds(2)).findActivities("Paris, France")).isEqualTo(activities);
    }

    @Test
    void returnsEmptyListWhenAmadeusFails() throws Exception {
        when(amadeus.findActivities(anyString(), anyInt())).thenThrow(mock(ResponseException.class));

        assertThat(service(Duration.ofSeconds(2)).findActivities("Paris")).isEmpty();
    }

    @Test
    void doesNotWaitLongerThanTheTimeout() throws Exception {
        when(amadeus.findActivities(anyString(), anyInt())).thenAnswer(invocation -> {
            Thread.sleep(2_000);
            return List.of();
        });

        long start = System.nanoTime();
        List<KnownActivity> result = service(Duration.ofMillis(200)).findActivities("Paris");

        assertThat(result).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void disabledMeansNoCall() {
        new ActivityGroundingService(amadeus, false, 10, Duration.ofSeconds(1)).findActivities("Paris");

        verifyNoInteractions(amadeus);
    }
}
