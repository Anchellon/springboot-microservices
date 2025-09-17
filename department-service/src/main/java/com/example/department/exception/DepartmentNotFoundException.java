package com.example.department.exception;

public class DepartmentNotFoundException extends RuntimeException {
    private final String resourceId;

    public DepartmentNotFoundException(String resourceId) {
        super("Department not found with id: " + resourceId);
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}