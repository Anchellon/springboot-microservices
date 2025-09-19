package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response following RFC 7807 Problem Details specification")
public class ErrorResponse {

    @Schema(description = "A URI reference that identifies the problem type",
            example = "/errors/validation-error")
    private String type;

    @Schema(description = "A short, human-readable summary of the problem type",
            example = "Validation Failed")
    private String title;

    @Schema(description = "The HTTP status code",
            example = "400")
    private int status;

    @Schema(description = "A human-readable explanation specific to this occurrence of the problem",
            example = "One or more fields have validation errors")
    private String detail;

    @Schema(description = "A URI reference that identifies the specific occurrence of the problem",
            example = "/api/v1/employees")
    private String instance;

    @Schema(description = "The timestamp when the error occurred in ISO 8601 format",
            example = "2023-12-01T10:30:00.123Z")
    private String timestamp;

    @Schema(description = "Unique identifier for tracing and debugging purposes",
            example = "abc123def456")
    private String traceId;

    @Schema(description = "List of field-specific validation errors")
    private List<FieldError> errors;

    @Schema(description = "Additional context or metadata about the error")
    private Map<String, Object> extensions;

    public ErrorResponse(String type, String title, int status, String detail, String instance, String traceId) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.timestamp = Instant.now().toString();
        this.traceId = traceId;
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Detailed information about a field validation error")
    public static class FieldError {

        @Schema(description = "The name of the field that failed validation",
                example = "email")
        private String field;

        @Schema(description = "The validation error message",
                example = "email must be valid")
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}