package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
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

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getInstance() { return instance; }
    public void setInstance(String instance) { this.instance = instance; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public List<FieldError> getErrors() { return errors; }
    public void setErrors(List<FieldError> errors) { this.errors = errors; }
    public Map<String, Object> getExtensions() { return extensions; }
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}