package com.back.atlas.controller;

import com.back.atlas.client.places.ActivityProvider;
import com.back.atlas.dto.Airport;
import com.back.atlas.dto.Place;
import com.back.atlas.service.AirportDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlacesController.class)
class PlacesControllerTest extends ControllerTestSupport {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ActivityProvider activityProvider;

    @MockitoBean
    AirportDirectory airportDirectory;

    private static List<Place> places(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> Place.builder().id("node/" + i).name("Place " + i).category("Museum").build())
                .toList();
    }

    @Test
    void activitiesRequireLogin() throws Exception {
        mvc.perform(get("/v1/api/activities").param("latitude", "41.9").param("longitude", "12.5"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(activityProvider);
    }

    @Test
    void activitiesArePagedAndTheRadiusIsInKilometres() throws Exception {
        when(activityProvider.nearby(eq(41.9), eq(12.5), eq(2000), anyInt())).thenReturn(places(15));

        mvc.perform(get("/v1/api/activities").with(loggedInAs(7))
                        .param("latitude", "41.9").param("longitude", "12.5")
                        .param("radius", "2").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("Place 11"))
                .andExpect(jsonPath("$.hasPrevious").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void invalidCoordinatesAreRejected() throws Exception {
        mvc.perform(get("/v1/api/activities").with(loggedInAs(7)).param("latitude", "120").param("longitude", "12.5"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(activityProvider);
    }

    @Test
    void providerDownIsA502WithAClearMessage() throws Exception {
        when(activityProvider.nearby(anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenThrow(new ActivityProvider.ProviderException("timeout", null));

        mvc.perform(get("/v1/api/activities").with(loggedInAs(7)).param("latitude", "41.9").param("longitude", "12.5"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Activities are temporarily unavailable, please try again"));
    }

    @Test
    void airportSearchIsPublic() throws Exception {
        when(airportDirectory.search("rome", 10)).thenReturn(List.of(
                new Airport("FCO", "Rome–Fiumicino", "Rome", "Italy", "IT", 41.8, 12.25)));

        mvc.perform(get("/v1/api/airports").param("keyword", "rome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iataCode").value("FCO"))
                .andExpect(jsonPath("$[0].cityName").value("Rome"));
    }
}
