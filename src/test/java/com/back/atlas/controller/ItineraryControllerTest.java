package com.back.atlas.controller;

import com.back.atlas.client.ai.AiServiceException;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.service.ItineraryGenerationService;
import com.back.atlas.service.ItineraryService;
import com.back.atlas.service.ItineraryStreamListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItineraryController.class)
class ItineraryControllerTest extends ControllerTestSupport {

    private static final String VALID_REQUEST = """
            {"destination": "Rome", "days": "2", "budget": "€500", "interests": ["food"]}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ItineraryService itineraryService;

    @MockitoBean
    ItineraryGenerationService generationService;

    private static GeneratedItinerary itinerary() {
        return GeneratedItinerary.builder().destination("Rome").totalDays(2).totalBudget("€500")
                .itinerary(List.of()).tips(List.of()).source("llm").build();
    }

    @Test
    void generateIsPublicAndUsesTheFrontendJsonKeys() throws Exception {
        when(generationService.generate(any())).thenReturn(itinerary());

        mvc.perform(post("/v1/api/itinerary/generate").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.itinerary.total_budget").value("€500"))
                .andExpect(jsonPath("$.itinerary.source").value("llm"));
    }

    @Test
    void invalidGenerateRequestReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/v1/api/itinerary/generate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destination\": \"\", \"days\": 30, \"budget\": \"500\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.destination").exists())
                .andExpect(jsonPath("$.fieldErrors.days").exists());
        verifyNoInteractions(generationService);
    }

    @Test
    void aiServiceDownReturns502() throws Exception {
        when(generationService.generate(any())).thenThrow(new AiServiceException("down", null));

        mvc.perform(post("/v1/api/itinerary/generate").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("The itinerary generator is temporarily unavailable"));
    }

    @Test
    void streamSendsServerSentEvents() throws Exception {
        doAnswer(invocation -> {
            ItineraryStreamListener listener = invocation.getArgument(1);
            listener.onStatus("working");
            listener.onResult(itinerary());
            return null;
        }).when(generationService).stream(any(), any());

        MvcResult result = mvc.perform(post("/v1/api/itinerary/generate/stream")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Wait for the result data, not just its "event:" line: the two are written separately.
        String body = awaitBody(result, "\"total_budget\"");
        assertThat(body).contains("event:status").contains("\"total_budget\":\"€500\"");
    }

    @Test
    void savedItinerariesRequireLogin() throws Exception {
        mvc.perform(get("/v1/api/itinerary/user")).andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInUserSeesOwnItineraries() throws Exception {
        when(itineraryService.getAllUserItineraries()).thenReturn(List.of(ItineraryResponse.builder().id(1L).build()));

        mvc.perform(get("/v1/api/itinerary/user").with(loggedInAs(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void cannotSaveForAnotherUser() throws Exception {
        mvc.perform(post("/v1/api/itinerary/save/8").with(loggedInAs(7))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(itineraryService);
    }

    @Test
    void canSaveForSelf() throws Exception {
        when(itineraryService.saveForCurrentUser(any())).thenReturn(ItineraryResponse.builder().id(3L).build());

        mvc.perform(post("/v1/api/itinerary/save/7").with(loggedInAs(7))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destination\": \"Rome\"}"))
                .andExpect(status().isOk());
    }

    private static String awaitBody(MvcResult result, String expected) throws Exception {
        for (int i = 0; i < 50; i++) {
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            if (body.contains(expected)) {
                return body;
            }
            Thread.sleep(50);
        }
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
