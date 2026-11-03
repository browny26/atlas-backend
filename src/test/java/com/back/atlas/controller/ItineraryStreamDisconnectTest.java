package com.back.atlas.controller;

import com.back.atlas.client.ai.AiServiceException;
import com.back.atlas.dto.AiItineraryRequest;
import com.back.atlas.dto.GeneratedItinerary;
import com.back.atlas.entity.DayItinerary;
import com.back.atlas.service.ItineraryGenerationService;
import com.back.atlas.service.ItineraryService;
import com.back.atlas.service.ItineraryStreamListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** What the stream does when the browser has already closed the connection. */
class ItineraryStreamDisconnectTest {

    private final ItineraryGenerationService generationService = mock(ItineraryGenerationService.class);
    private final ItineraryController controller =
            new ItineraryController(mock(ItineraryService.class), generationService);
    private final AiItineraryRequest request = new AiItineraryRequest();
    private SseEmitter emitter;

    @BeforeEach
    void setUp() {
        emitter = mock(SseEmitter.class);
    }

    /** The service sends one day, then the result: the second write must never happen. */
    private void serviceSendsADayThenTheResult() {
        doAnswer(invocation -> {
            ItineraryStreamListener listener = invocation.getArgument(1);
            listener.onDay(new DayItinerary());
            listener.onResult(GeneratedItinerary.builder().build());
            return null;
        }).when(generationService).stream(any(), any());
    }

    @Test
    void brokenPipeStopsTheStreamWithoutWritingAnError() throws Exception {
        serviceSendsADayThenTheResult();
        doThrow(new IOException("Broken pipe")).when(emitter).send(any(SseEventBuilder.class));

        assertThatCode(() -> controller.runStream(request, emitter)).doesNotThrowAnyException();

        verify(emitter, times(1)).send(any(SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }

    @Test
    void writingToACompletedEmitterStopsTheStreamWithoutWritingAnError() throws Exception {
        serviceSendsADayThenTheResult();
        doThrow(new IllegalStateException("ResponseBodyEmitter has already completed"))
                .when(emitter).send(any(SseEventBuilder.class));

        assertThatCode(() -> controller.runStream(request, emitter)).doesNotThrowAnyException();

        verify(emitter, times(1)).send(any(SseEventBuilder.class));
        verify(emitter, never()).complete();
    }

    @Test
    void disconnectWhileSendingTheErrorIsIgnored() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget"))
                .when(generationService).stream(any(), any());
        doThrow(new IOException("Broken pipe")).when(emitter).send(any(SseEventBuilder.class));

        assertThatCode(() -> controller.runStream(request, emitter)).doesNotThrowAnyException();

        verify(emitter, times(1)).send(any(SseEventBuilder.class));
        verify(emitter, never()).complete();
    }

    @Test
    void aiFailureIsReportedToAConnectedClient() throws Exception {
        doThrow(new AiServiceException("down", null)).when(generationService).stream(any(), any());

        controller.runStream(request, emitter);

        verify(emitter, times(1)).send(any(SseEventBuilder.class));
        verify(emitter, times(1)).complete();
    }
}
