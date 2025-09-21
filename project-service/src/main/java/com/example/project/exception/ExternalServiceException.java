package com.example.project.exception;

import lombok.Getter;

/**
 * Exception for external service failures (503)
 * Used when Employee API is unavailable or returns 5xx errors
 */
@Getter
public class ExternalServiceException extends RuntimeException {
    private final String serviceName;

    public ExternalServiceException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String message, String serviceName, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }
}