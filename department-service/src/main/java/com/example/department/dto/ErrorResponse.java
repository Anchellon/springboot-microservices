// ========================================
// ERROR RESPONSE DTO
// ========================================
package com.example.department.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error response with guidance for resolution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Schema(description = "Error type URI", example = "/errors/validation-error")
    private String type;

    @Schema(description = "Error title", example = "Validation Failed")
    private String title;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Detailed error message", example = "One or more fields have validation errors")
    private String detail;

    @Schema(description = "Request URI where error occurred", example = "/api/v1/departments")
    private String instance;

    @Schema(description = "Error timestamp in ISO format", example = "2023-12-01T10:30:00.123Z")
    @Builder.Default
    private String timestamp = Instant.now().toString();

    @Schema(description = "Trace ID for debugging", example = "abc123def456")
    private String traceId;

    @Schema(description = "Field-specific validation errors")
    private List<FieldError> errors;

    @Schema(description = "Helpful guidance on how to resolve the error",
            example = "Please correct the validation errors and retry the request")
    private String guidance;

    @Schema(description = "Field validation error details")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        @Schema(description = "Field name that failed validation", example = "code")
        private String field;

        @Schema(description = "Validation error message", example = "Code must be 2-10 uppercase letters")
        private String message;
    }
}