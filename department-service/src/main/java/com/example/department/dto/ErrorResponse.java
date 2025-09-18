package com.example.department.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private String traceId;
    private List<FieldError> errors;
    private String guidance; // Helpful instructions for resolution

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;
    }
}