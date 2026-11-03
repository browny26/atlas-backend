package com.back.atlas.service;

import com.back.atlas.client.ai.AiItineraryClient;
import com.back.atlas.client.ai.AiRegenerateDayRequest;
import com.back.atlas.client.ai.AiServiceException;
import com.back.atlas.client.ai.AiServiceRequest;
import com.back.atlas.client.ai.AiServiceRequest.KnownActivity;
import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.client.ai.AiStreamListener;
import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.dto.RegenerateDayRequest;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.dto.SaveItineraryRequest.DayItineraryDTO;
import com.back.atlas.dto.SaveItineraryRequest.DayItineraryDTO.ActivityDTO;
import com.back.atlas.entity.DayItinerary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItineraryGenerationServiceTest {

    private static final List<KnownActivity> ACTIVITIES = List.of(new KnownActivity("Colosseum tour", 45.0));

    private final AiItineraryClient client = mock(AiItineraryClient.class);
    private final ActivityGroundingService grounding = mock(ActivityGroundingService.class);
    private final ItineraryGenerationService service = new ItineraryGenerationService(client, grounding);

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
    void sendsNormalizedAndGroundedRequestToAiService() {
        when(grounding.findActivities("Rome")).thenReturn(ACTIVITIES);
        when(client.generateItinerary(any())).thenReturn(sampleResponse());

        GeneratedItinerary result = service.generate(request("  Rome ", "€300"));

        ArgumentCaptor<AiServiceRequest> sent = ArgumentCaptor.forClass(AiServiceRequest.class);
        verify(client).generateItinerary(sent.capture());
        assertThat(sent.getValue()).isEqualTo(new AiServiceRequest("Rome", 1, 300, List.of(), ACTIVITIES));
        assertThat(result.getDestination()).isEqualTo("Rome");
    }

    @Test
    void propagatesAiServiceFailure() {
        when(client.generateItinerary(any())).thenThrow(new AiServiceException("down", null));

        assertThatThrownBy(() -> service.generate(request("Rome", "300")))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void streamMapsEventsToFrontendFormat() {
        doAnswer(invocation -> {
            AiStreamListener listener = invocation.getArgument(1);
            listener.onDay(sampleResponse().days().getFirst());
            listener.onResult(sampleResponse());
            return null;
        }).when(client).streamItinerary(any(), any());
        List<DayItinerary> days = new ArrayList<>();
        List<GeneratedItinerary> results = new ArrayList<>();

        service.stream(request("Rome", "300"), new ItineraryStreamListener() {
            public void onDay(DayItinerary day) {
                days.add(day);
            }

            public void onStatus(String message) {
            }

            public void onResult(GeneratedItinerary itinerary) {
                results.add(itinerary);
            }
        });

        assertThat(days).singleElement().satisfies(d -> assertThat(d.getMorning().getCost()).isEqualTo("€10"));
        assertThat(results).singleElement().satisfies(r -> assertThat(r.getTotalEstimatedCost()).isEqualTo("€130"));
    }

    @Test
    void regenerateDayConvertsTheDisplayedItineraryBackToNumbers() {
        when(client.regenerateDay(any())).thenReturn(sampleResponse());
        RegenerateDayRequest request = new RegenerateDayRequest();
        request.setItinerary(displayedItinerary());
        request.setDay(1);
        request.setFeedback("more food");

        service.regenerateDay(request);

        ArgumentCaptor<AiRegenerateDayRequest> sent = ArgumentCaptor.forClass(AiRegenerateDayRequest.class);
        verify(client).regenerateDay(sent.capture());
        AiRegenerateDayRequest aiRequest = sent.getValue();
        assertThat(aiRequest.trip().budgetEur()).isEqualTo(300);
        assertThat(aiRequest.plan().days().getFirst().morning().costEur()).isEqualTo(16);
        assertThat(aiRequest.plan().days().getFirst().evening().time()).isEqualTo("20:00"); // was invalid
        assertThat(aiRequest.plan().accommodation().costPerNightEur()).isEqualTo(110);
        assertThat(aiRequest.feedback()).isEqualTo("more food");
        verifyNoInteractions(grounding);
    }

    @Test
    void blankFeedbackIsSentAsNoFeedback() {
        when(client.regenerateDay(any())).thenReturn(sampleResponse());
        RegenerateDayRequest request = new RegenerateDayRequest();
        request.setItinerary(displayedItinerary());
        request.setDay(1);
        request.setFeedback("   ");

        service.regenerateDay(request);

        ArgumentCaptor<AiRegenerateDayRequest> sent = ArgumentCaptor.forClass(AiRegenerateDayRequest.class);
        verify(client).regenerateDay(sent.capture());
        assertThat(sent.getValue().feedback()).isNull();
    }

    @Test
    void regenerateDayRejectsNullDays() {
        SaveItineraryRequest itinerary = displayedItinerary();
        itinerary.setItinerary(java.util.Arrays.asList((DayItineraryDTO) null));
        RegenerateDayRequest request = new RegenerateDayRequest();
        request.setItinerary(itinerary);
        request.setDay(1);

        assertThatThrownBy(() -> service.regenerateDay(request)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(client);
    }

    @Test
    void regenerateDayRejectsUnknownDay() {
        RegenerateDayRequest request = new RegenerateDayRequest();
        request.setItinerary(displayedItinerary());
        request.setDay(2);

        assertThatThrownBy(() -> service.regenerateDay(request)).isInstanceOf(ResponseStatusException.class);
    }

    private static AiItineraryRequest request(String destination, String budget) {
        AiItineraryRequest request = new AiItineraryRequest();
        request.setDestination(destination);
        request.setDays(1);
        request.setBudget(budget);
        return request;
    }

    private static SaveItineraryRequest displayedItinerary() {
        SaveItineraryRequest itinerary = new SaveItineraryRequest();
        itinerary.setDestination("Rome");
        itinerary.setTotalBudget("€300");
        itinerary.setAccommodation(new SaveItineraryRequest.AccommodationDTO("Hotel", "€110/night", null, null));
        itinerary.setItinerary(List.of(new DayItineraryDTO(1,
                new ActivityDTO("Colosseum", "€16", "09:00"),
                new ActivityDTO("Pantheon", "€0", "14:00"),
                new ActivityDTO("Dinner", "€20-€40", "evening"))));
        itinerary.setTips(List.of("Tip"));
        return itinerary;
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
