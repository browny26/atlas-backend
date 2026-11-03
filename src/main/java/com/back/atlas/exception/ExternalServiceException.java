package com.back.atlas.exception;

/** A third-party service (storage, e-mail, ...) failed. Mapped to 502 Bad Gateway. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
