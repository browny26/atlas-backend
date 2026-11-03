package com.back.atlas.client.ai;

import com.back.atlas.config.AiServiceConfig;
import com.back.atlas.config.AiServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiItineraryClientTest {

    private static final String RESPONSE = """
            {
              "destination": "Rome",
              "total_days": 1,
              "currency": "EUR",
              "budget_eur": 300.0,
              "nights": 1,
              "total_estimated_cost_eur": 151.5,
              "budget_status": "within_budget",
              "days": [{
                "day": 1,
                "morning":   {"activity": "Colosseum", "time": "09:00", "cost_eur": 18.0},
                "afternoon": {"activity": "Pantheon",  "time": "14:00", "cost_eur": 0.0},
                "evening":   {"activity": "Dinner",    "time": "20:00", "cost_eur": 33.5}
              }],
              "accommodation": {"name": "B&B Roma", "type": "B&B", "details": "Monti", "cost_per_night_eur": 100.0},
              "tips": ["Book online"],
              "metadata": {"source": "llm", "model": "llama", "prompt_version": "v2",
                           "attempts": 1, "latency_ms": 1200, "usage": {"total_tokens": 900}}
            }
            """;

    private MockRestServiceServer server;
    private AiItineraryClient client;

    @BeforeEach
    void setUp() {
        AiServiceProperties properties = new AiServiceProperties("http://ai.test", "secret", Duration.ofSeconds(5));
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiItineraryClient(AiServiceConfig.configure(builder, properties).build());
    }

    @Test
    void sendsSnakeCaseRequestWithApiKeyAndParsesResponse() {
        server.expect(requestTo("http://ai.test/v1/itineraries"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "secret"))
                .andExpect(jsonPath("$.budget_eur").value(300.0))
                .andExpect(jsonPath("$.days").value(1))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        AiServiceResponse response = client.generateItinerary(
                new AiServiceRequest("Rome", 1, 300, List.of("history"), List.of()));

        server.verify();
        assertThat(response.totalEstimatedCostEur()).isEqualTo(151.5);
        assertThat(response.days().getFirst().morning().costEur()).isEqualTo(18.0);
        assertThat(response.accommodation().costPerNightEur()).isEqualTo(100.0);
        assertThat(response.metadata().source()).isEqualTo("llm");
    }

    @Test
    void streamParsesServerSentEvents() {
        String day = """
                {"day": 1, "morning": {"activity": "Colosseum", "time": "09:00", "cost_eur": 18.0},
                 "afternoon": {"activity": "Pantheon", "time": "14:00", "cost_eur": 0.0},
                 "evening": {"activity": "Dinner", "time": "20:00", "cost_eur": 33.5}}""".replace("\n", "");
        String body = "event: day\ndata: " + day + "\n\n"
                + "event: status\ndata: {\"message\": \"Fixing\"}\n\n"
                + "event: result\ndata: " + RESPONSE.replace("\n", "") + "\n\n";
        server.expect(requestTo("http://ai.test/v1/itineraries/stream"))
                .andRespond(withSuccess(body, MediaType.TEXT_EVENT_STREAM));
        List<String> events = new ArrayList<>();

        client.streamItinerary(new AiServiceRequest("Rome", 1, 300, List.of(), List.of()), new AiStreamListener() {
            public void onDay(AiServiceResponse.Day d) {
                events.add("day:" + d.morning().activity());
            }

            public void onStatus(String message) {
                events.add("status:" + message);
            }

            public void onResult(AiServiceResponse result) {
                events.add("result:" + result.totalEstimatedCostEur());
            }
        });

        assertThat(events).containsExactly("day:Colosseum", "status:Fixing", "result:151.5");
    }

    @Test
    void streamErrorEventBecomesAiServiceException() {
        server.expect(requestTo("http://ai.test/v1/itineraries/stream"))
                .andRespond(withSuccess("event: error\ndata: {\"detail\": \"down\"}\n\n", MediaType.TEXT_EVENT_STREAM));

        assertThatThrownBy(() -> client.streamItinerary(
                new AiServiceRequest("Rome", 1, 300, List.of(), List.of()), mock(AiStreamListener.class)))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void listenerExceptionsEscapeTheStreamUnwrapped() {
        // The controller throws from the listener when the browser has gone: that must not
        // be turned into an AiServiceException, or it would be reported as an AI failure.
        server.expect(requestTo("http://ai.test/v1/itineraries/stream"))
                .andRespond(withSuccess("event: status\ndata: {\"message\": \"working\"}\n\n", MediaType.TEXT_EVENT_STREAM));
        RuntimeException clientGone = new RuntimeException("client disconnected");
        AiStreamListener listener = mock(AiStreamListener.class);
        doThrow(clientGone).when(listener).onStatus(any());

        assertThatThrownBy(() -> client.streamItinerary(
                new AiServiceRequest("Rome", 1, 300, List.of(), List.of()), listener))
                .isSameAs(clientGone);
    }

    @Test
    void streamEndingWithoutResultIsAFailure() {
        server.expect(requestTo("http://ai.test/v1/itineraries/stream"))
                .andRespond(withSuccess("event: status\ndata: {\"message\": \"working\"}\n\n", MediaType.TEXT_EVENT_STREAM));

        assertThatThrownBy(() -> client.streamItinerary(
                new AiServiceRequest("Rome", 1, 300, List.of(), List.of()), mock(AiStreamListener.class)))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void sendsKnownActivitiesInSnakeCase() {
        server.expect(requestTo("http://ai.test/v1/itineraries"))
                .andExpect(jsonPath("$.known_activities[0].name").value("Colosseum tour"))
                .andExpect(jsonPath("$.known_activities[0].price_eur").value(45.0))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        client.generateItinerary(new AiServiceRequest("Rome", 1, 300, List.of(),
                List.of(new AiServiceRequest.KnownActivity("Colosseum tour", 45.0))));

        server.verify();
    }

    @Test
    void serverErrorBecomesAiServiceException() {
        server.expect(requestTo("http://ai.test/v1/itineraries")).andRespond(withServerError());

        assertThatThrownBy(() -> client.generateItinerary(new AiServiceRequest("Rome", 1, 300, List.of(), List.of())))
                .isInstanceOf(AiServiceException.class);
    }
}
