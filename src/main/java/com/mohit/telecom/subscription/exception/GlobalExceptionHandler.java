package com.mohit.telecom.subscription.exception;

import com.mohit.telecom.subscription.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler
 * Centralized exception handling for the Telecom Subscription Service REST API.
 * Translates exceptions into consistent ErrorResponse payloads with appropriate HTTP status codes.
 * @author mohit
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles TelecomException — business logic validation failures.
     * @param ex      the TelecomException
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and the exception's HTTP status
     */
    @ExceptionHandler(TelecomException.class)
    public ResponseEntity<ErrorResponse> handleTelecomException(TelecomException ex, HttpServletRequest request) {
        LOG.info("GlobalExceptionHandler:: handleTelecomException method started");
        LOG.error("Exception in handleTelecomException: ", ex);
        ErrorResponse error = ErrorResponse.builder()
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    /**
     * Handles MethodArgumentNotValidException — Bean Validation failures on request bodies.
     * Collects all field-level validation errors into a single message.
     * @param ex      the validation exception
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and 400 Bad Request status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        LOG.info("GlobalExceptionHandler:: handleValidationException method started");
        LOG.error("Exception in handleValidationException: ", ex);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles all uncaught exceptions as a fallback, returning 500 Internal Server Error.
     * @param ex      the unexpected exception
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        LOG.info("GlobalExceptionHandler:: handleGenericException method started");
        LOG.error("Exception in handleGenericException: ", ex);
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
