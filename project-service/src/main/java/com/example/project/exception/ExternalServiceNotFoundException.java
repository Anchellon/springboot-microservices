
package com.example.project.exception;

import lombok.Getter;
/**
 * Exception for external service resource not found (404)
 * Used when Employee API returns 404 for an employeeId
 */
@Getter
public class ExternalServiceNotFoundException extends RuntimeException {
    private final String serviceName;

    public ExternalServiceNotFoundException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
    }
}