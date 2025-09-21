package com.example.project.exception;

public class ProjectMemberNotFoundException extends RuntimeException {

    public ProjectMemberNotFoundException(Long projectId, Long employeeId) {
        super(String.format("Employee %d is not a member of project %d", employeeId, projectId));
    }

    public ProjectMemberNotFoundException(String message) {
        super(message);
    }
}