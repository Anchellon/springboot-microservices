package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    // Getters and setters
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String timestamp;
    private String traceId;
    private List<FieldError> errors;
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

    public void setType(String type) { this.type = type; }

    public void setTitle(String title) { this.title = title; }

    public void setStatus(int status) { this.status = status; }

    public void setDetail(String detail) { this.detail = detail; }

    public void setInstance(String instance) { this.instance = instance; }

    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public void setTraceId(String traceId) { this.traceId = traceId; }

    public void setErrors(List<FieldError> errors) { this.errors = errors; }

    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public void setField(String field) { this.field = field; }

        public void setMessage(String message) { this.message = message; }
    }
}