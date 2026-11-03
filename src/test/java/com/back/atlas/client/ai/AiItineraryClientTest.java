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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                new AiServiceRequest("Rome", 1, 300, List.of("history")));

        server.verify();
        assertThat(response.totalEstimatedCostEur()).isEqualTo(151.5);
        assertThat(response.days().getFirst().morning().costEur()).isEqualTo(18.0);
        assertThat(response.accommodation().costPerNightEur()).isEqualTo(100.0);
        assertThat(response.metadata().source()).isEqualTo("llm");
    }

    @Test
    void serverErrorBecomesAiServiceException() {
        server.expect(requestTo("http://ai.test/v1/itineraries")).andRespond(withServerError());

        assertThatThrownBy(() -> client.generateItinerary(new AiServiceRequest("Rome", 1, 300, List.of())))
                .isInstanceOf(AiServiceException.class);
    }
}
