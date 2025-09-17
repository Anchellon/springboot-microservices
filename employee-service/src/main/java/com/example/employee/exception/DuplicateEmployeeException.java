package com.example.employee.exception;

public class DuplicateEmployeeException extends RuntimeException {
    private final String field;
    private final String value;

    public DuplicateEmployeeException(String field, String value) {
        super("Employee already exists with " + field + ": " + value);
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }
}