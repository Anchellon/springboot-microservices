package com.example.employee.exception;


public class EmployeeNotFoundException extends RuntimeException {
    private final String resourceId;

    public EmployeeNotFoundException(String resourceId) {
        super("Employee not found with id: " + resourceId);
        this.resourceId = resourceId;
    }

}
