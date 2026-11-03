package com.back.atlas.service;

import com.back.atlas.client.ai.AiItineraryClient;
import com.back.atlas.client.ai.AiServiceException;
import com.back.atlas.client.ai.AiServiceRequest;
import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.dto.AiItineraryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItineraryGenerationServiceTest {

    private final AiItineraryClient client = mock(AiItineraryClient.class);
    private final ItineraryGenerationService service = new ItineraryGenerationService(client);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"500|500", "€500|500", "500 €|500", "499.90|499.9", "499,90|499.9"})
    void parsesBudget(String input, double expected) {
        assertThat(ItineraryGenerationService.parseBudget(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-", "€"})
    void rejectsInvalidBudget(String input) {
        assertThatThrownBy(() -> ItineraryGenerationService.parseBudget(input))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void sendsNormalizedRequestToAiService() {
        when(client.generateItinerary(any())).thenReturn(sampleResponse());
        AiItineraryRequest request = new AiItineraryRequest();
        request.setDestination("  Rome ");
        request.setDays(1);
        request.setBudget("€300");

        var result = service.generate(request);

        ArgumentCaptor<AiServiceRequest> sent = ArgumentCaptor.forClass(AiServiceRequest.class);
        verify(client).generateItinerary(sent.capture());
        assertThat(sent.getValue()).isEqualTo(new AiServiceRequest("Rome", 1, 300, List.of()));
        assertThat(result.getDestination()).isEqualTo("Rome");
    }

    @Test
    void propagatesAiServiceFailure() {
        when(client.generateItinerary(any())).thenThrow(new AiServiceException("down", null));
        AiItineraryRequest request = new AiItineraryRequest();
        request.setDestination("Rome");
        request.setDays(1);
        request.setBudget("300");

        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(AiServiceException.class);
    }

    private static AiServiceResponse sampleResponse() {
        var activity = new AiServiceResponse.Activity("Walk", "10:00", 10);
        return new AiServiceResponse("Rome", 1, "EUR", 300, 1, 130, "within_budget",
                List.of(new AiServiceResponse.Day(1, activity, activity, activity)),
                new AiServiceResponse.Accommodation("Hotel", "hotel", "Centre", 100),
                List.of("Tip"),
                new AiServiceResponse.Metadata("llm", "llama", "v2", 1, 900));
    }
}
