package com.mohit.telecom.subscription.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-specific exception that carries an HTTP status, an internal error code,
 * and a human-readable message. Used throughout the service layer to bubble up
 * business rule violations to the controller tier.
 */
public class TelecomException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public TelecomException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
