
package com.example.project.exception;

/**
 * Exception thrown when a project is not found by its ID.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project not found");
    }
}