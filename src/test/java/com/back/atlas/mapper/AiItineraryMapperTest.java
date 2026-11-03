package com.back.atlas.mapper;

import com.back.atlas.client.ai.AiServiceResponse;
import com.back.atlas.client.ai.AiServiceResponse.Activity;
import com.back.atlas.dto.GeneratedItinerary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiItineraryMapperTest {

    @ParameterizedTest
    @CsvSource({"16.0, €16", "16.5, €16.50", "0, €0", "1600, €1600", "12.345, €12.35"})
    void formatsEuros(double amount, String expected) {
        assertThat(AiItineraryMapper.euros(amount)).isEqualTo(expected);
    }

    @Test
    void mapsToFrontendShape() {
        AiServiceResponse response = new AiServiceResponse(
                "Rome", 1, "EUR", 300, 1, 151.5, "within_budget",
                List.of(new AiServiceResponse.Day(1,
                        new Activity("Colosseum", "09:00", 18),
                        new Activity("Pantheon", "14:00", 0),
                        new Activity("Dinner", "20:00", 33.5))),
                new AiServiceResponse.Accommodation("B&B Roma", "B&B", "Monti", 100),
                List.of("Book online"),
                new AiServiceResponse.Metadata("llm", "llama", "v2", 1, 1200));

        GeneratedItinerary result = AiItineraryMapper.toGeneratedItinerary(response, List.of("history"));

        assertThat(result.getTotalBudget()).isEqualTo("€300");
        assertThat(result.getTotalEstimatedCost()).isEqualTo("€151.50");
        assertThat(result.getItinerary().getFirst().getMorning().getCost()).isEqualTo("€18");
        assertThat(result.getItinerary().getFirst().getEvening().getTime()).isEqualTo("20:00");
        assertThat(result.getAccommodation().getCost()).isEqualTo("€100/night");
        assertThat(result.getSource()).isEqualTo("llm");
    }
}
