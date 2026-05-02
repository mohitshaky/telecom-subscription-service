package com.mohit.telecom.subscription.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ErrorResponse - Standard error payload returned by GlobalExceptionHandler.
 * @author mohit
 */
@Getter
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
