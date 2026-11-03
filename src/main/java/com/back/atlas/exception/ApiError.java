package com.back.atlas.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body. The frontend reads error.response.data.message.
 * A plain class (not a record) because the app serializes with Gson.
 */
public class ApiError {
    private final int status;
    private final String error;
    private final String message;
    private final Map<String, String> fieldErrors;
    private final String timestamp = Instant.now().toString();

    public ApiError(int status, String error, String message, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
