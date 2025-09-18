package com.example.department.exception;

import com.example.department.dto.ErrorResponse;
import com.example.department.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_TYPE = "/errors/validation-error";
    private static final String NOT_FOUND_TYPE = "/errors/not-found";
    private static final String CONFLICT_TYPE = "/errors/conflict";
    private static final String DEPARTMENT_IN_USE_TYPE = "/errors/department-in-use";
    private static final String INTERNAL_ERROR_TYPE = "/errors/internal-error";

    // ========================================
    // VALIDATION ERRORS (400 BAD REQUEST)
    // ========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Validation error on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(VALIDATION_ERROR_TYPE)
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more fields have validation errors")
                .instance(request.getRequestURI())
                .traceId(traceId)
                .errors(fieldErrors)
                .guidance("Please correct the validation errors and retry the request")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========================================
    // NOT FOUND ERRORS (404 NOT FOUND)
    // ========================================
    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            DepartmentNotFoundException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Department not found on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(NOT_FOUND_TYPE)
                .title("Department Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .traceId(traceId)
                .guidance("Please verify the department ID and try again")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // ========================================
    // DUPLICATE RESOURCE ERRORS (409 CONFLICT)
    // ========================================
    @ExceptionHandler(DuplicateDepartmentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDepartment(
            DuplicateDepartmentException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Duplicate department on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        String guidance = ex.getField().equals("name")
                ? "Please choose a different department name"
                : "Please choose a different department code";

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(CONFLICT_TYPE)
                .title("Duplicate Department")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .traceId(traceId)
                .guidance(guidance)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ========================================
    // PROTECTIVE DELETE ERRORS (409 CONFLICT)
    // ========================================
    @ExceptionHandler(DepartmentInUseException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentInUse(
            DepartmentInUseException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Department deletion blocked [traceId={}]: {}", traceId, ex.getMessage());

        String guidance = String.format(
                "To delete this department: " +
                        "1. Reassign all %d employees to other departments, " +
                        "2. Or remove the employees first, " +
                        "3. Then retry the department deletion. " +
                        "Use GET /employees?departmentId=%d to see affected employees.",
                ex.getEmployeeCount(),
                ex.getDepartmentId()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEPARTMENT_IN_USE_TYPE)
                .title("Department Cannot Be Deleted")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .traceId(traceId)
                .guidance(guidance)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ========================================
    // BUSINESS RULE VIOLATIONS (409 CONFLICT)
    // ========================================
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.warn("Business rule violation on {} [traceId={}]: {}", request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(CONFLICT_TYPE)
                .title("Business Rule Violation")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .traceId(traceId)
                .guidance("Please review the business rules and adjust your request")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ========================================
    // GENERIC ERRORS (500 INTERNAL SERVER ERROR)
    // ========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String traceId = TraceIdUtil.getCurrentTraceId();
        log.error("Unexpected error on {} [traceId={}]", request.getRequestURI(), traceId, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(INTERNAL_ERROR_TYPE)
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred. Please try again later.")
                .instance(request.getRequestURI())
                .traceId(traceId)
                .guidance("If the problem persists, please contact support with trace ID: " + traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

