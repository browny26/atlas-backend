package com.back.atlas.exception;

import com.back.atlas.client.ai.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps exceptions to HTTP status codes in one place, so controllers have no try/catch.
 * Unexpected errors are logged with their stack trace but never shown to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        return build(ex.getStatusCode(), ex.getReason(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Invalid request", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", null);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiError> handleAiService(AiServiceException ex) {
        return build(HttpStatus.BAD_GATEWAY, "The itinerary generator is temporarily unavailable", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Spring MVC exceptions (404 no handler, 405 method not allowed, missing parameter, ...)
        // already carry the right status.
        if (ex instanceof ErrorResponse springError) {
            return build(springError.getStatusCode(), springError.getBody().getDetail(), null);
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null);
    }

    private static ResponseEntity<ApiError> build(HttpStatusCode status, String message,
                                                       Map<String, String> fieldErrors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String error = resolved != null ? resolved.getReasonPhrase() : "Error";
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), error, message, fieldErrors));
    }
}
