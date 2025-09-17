package com.example.department.exception;

public class DuplicateDepartmentException extends RuntimeException {
    private final String field;
    private final String value;

    public DuplicateDepartmentException(String field, String value) {
        super("Department already exists with " + field + ": " + value);
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }
}