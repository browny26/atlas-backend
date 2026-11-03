package com.back.atlas.client.ai;

/** Receives the events of a streamed generation, in order. */
public interface AiStreamListener {
    /** A validated day, sent as soon as the model has written it. */
    void onDay(AiServiceResponse.Day day);

    /** A progress message (e.g. while an invalid answer is being repaired). */
    void onStatus(String message);

    /** The final, authoritative itinerary. */
    void onResult(AiServiceResponse result);
}
