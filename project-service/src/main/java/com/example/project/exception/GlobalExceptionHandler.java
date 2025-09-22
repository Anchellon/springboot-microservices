package com.example.project.exception;

import jakarta.persistence.EntityNotFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 404 Not Found - Project not found
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProjectNotFoundException(ProjectNotFoundException ex) {
        log.warn("Project not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    // 404 Not Found - Generic entity not found (fallback)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    // 400 Bad Request - Input validation failures (controller-level)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument provided: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // 400 Bad Request - Validation errors on request body (legacy)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        // Create errors array with field and message objects
        var errorList = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", ((FieldError) error).getField());
                    errorMap.put("message", error.getDefaultMessage());
                    return errorMap;
                })
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid payload");
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errors", errorList);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // 400 Bad Request - Spring Boot 3.x validation errors
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        log.warn("Handler method validation error: {}", ex.getMessage());

        // Extract validation errors using correct method calls
        List<Map<String, String>> errorList = ex.getValueResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    String fieldName = "unknown";
                    String message = error.getDefaultMessage();

                    // Check if it's a FieldError to get the field name
                    if (error instanceof FieldError fieldError) {
                        fieldName = fieldError.getField();
                    } else {
                        // For other types of errors, use the codes to try to extract field info
                        String[] codes = error.getCodes();
                        if (codes != null) {
                            // Try to extract field name from error codes
                            for (String code : codes) {
                                if (code.contains(".")) {
                                    String[] parts = code.split("\\.");
                                    if (parts.length > 1) {
                                        fieldName = parts[parts.length - 1];
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    errorMap.put("field", fieldName);
                    errorMap.put("message", message != null ? message : "Validation failed");
                    return errorMap;
                })
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid payload");
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errors", errorList);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // 400 Bad Request - Constraint violations (e.g., @AssertTrue custom validations)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        // Create errors array with field and message objects
        var errorList = ex.getConstraintViolations().stream()
                .map(violation -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", violation.getPropertyPath().toString());
                    errorMap.put("message", violation.getMessage());
                    return errorMap;
                })
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid payload");
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errors", errorList);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // 400 Bad Request - Invalid request format/parsing
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed request: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid payload");
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // 400 Bad Request - Type mismatch (e.g., invalid enum values, wrong data types)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid payload");
        problemDetail.setTitle("Invalid Parameter Type");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // 409 Conflict - Business logic conflicts
    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ProblemDetail> handleBusinessConflictException(BusinessConflictException ex) {
        log.warn("Business conflict: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Conflict");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        // Add specific conflict details if provided
        if (ex.getConflictDetails() != null) {
            problemDetail.setProperty("conflictDetails", ex.getConflictDetails());
        }

        // Add suggested actions if provided
        if (ex.getSuggestedActions() != null && !ex.getSuggestedActions().isEmpty()) {
            problemDetail.setProperty("suggestedActions", ex.getSuggestedActions());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    // 409 Conflict - Database constraint violations (unique constraints, foreign keys)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());

        String message = "Unique constraint violation";

        // Check for common constraint violations and provide specific messages
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage().toLowerCase() : "";

        if (rootCauseMessage.contains("project_code") || rootCauseMessage.contains("code")) {
            message = "Project with this code already exists";
        } else if (rootCauseMessage.contains("uk_project_employee") ||
                (rootCauseMessage.contains("project_id") && rootCauseMessage.contains("employee_id"))) {
            message = "Employee is already a member of this project";
        } else if (rootCauseMessage.contains("unique constraint") || rootCauseMessage.contains("unique")) {
            message = "Duplicate entry violates unique constraint";
        } else if (rootCauseMessage.contains("foreign key") || rootCauseMessage.contains("fk_")) {
            message = "Cannot perform operation due to existing relationships";
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, message);
        problemDetail.setTitle("Conflict");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    // 404 Not Found - External service dependency not found
    @ExceptionHandler(ExternalServiceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceNotFoundException(ExternalServiceNotFoundException ex) {
        log.warn("External service resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("External Resource Not Found");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("service", ex.getServiceName());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    // 503 Service Unavailable - External service failures
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceException(ExternalServiceException ex) {
        log.error("External service error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "External service temporarily unavailable");
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("service", ex.getServiceName());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }

    // 404 Not Found - Project member not found
    @ExceptionHandler(ProjectMemberNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProjectMemberNotFoundException(ProjectMemberNotFoundException ex) {
        log.warn("Project member not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problemDetail.setTitle("Project Member Not Found");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    // 500 Internal Server Error - Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        // Explicitly NOT including stack trace or detailed error information

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}