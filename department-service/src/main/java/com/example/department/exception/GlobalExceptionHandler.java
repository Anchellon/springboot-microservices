package com.example.department.exception;

import com.example.department.dto.ErrorResponse;
import com.example.department.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_TYPE = "/errors/validation-error";
    private static final String NOT_FOUND_TYPE = "/errors/not-found";
    private static final String CONFLICT_TYPE = "/errors/conflict";
    private static final String INTERNAL_ERROR_TYPE = "/errors/internal-error";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Validation error on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                VALIDATION_ERROR_TYPE,
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "One or more fields have validation errors",
                request.getRequestURI(),
                traceId
        );
        errorResponse.setErrors(fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({DepartmentNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Resource not found on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                NOT_FOUND_TYPE,
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler({DuplicateDepartmentException.class, BusinessRuleViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolations(
            RuntimeException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Business rule violation on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                CONFLICT_TYPE,
                "Business Rule Violation",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.error("Unexpected error on {} [traceId={}]", request.getRequestURI(), traceId, ex);

        ErrorResponse errorResponse = new ErrorResponse(
                INTERNAL_ERROR_TYPE,
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}