package com.back.atlas.client.ai;

/** The AI service is unreachable or answered with an error. */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
