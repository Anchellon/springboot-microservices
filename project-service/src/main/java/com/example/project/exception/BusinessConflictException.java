package com.example.project.exception;

import lombok.Getter;
import java.util.List;
import java.util.Map;

/**
 * Exception for business logic conflicts (409 Conflict)
 * Used for cases like:
 * - Employee already assigned to project
 * - Cannot delete project with active members
 * - Status transition not allowed
 */
@Getter
public class BusinessConflictException extends RuntimeException {
    private final Map<String, Object> conflictDetails;
    private final List<String> suggestedActions;

    public BusinessConflictException(String message) {
        super(message);
        this.conflictDetails = null;
        this.suggestedActions = null;
    }

    public BusinessConflictException(String message, Map<String, Object> conflictDetails) {
        super(message);
        this.conflictDetails = conflictDetails;
        this.suggestedActions = null;
    }

    public BusinessConflictException(String message, List<String> suggestedActions) {
        super(message);
        this.conflictDetails = null;
        this.suggestedActions = suggestedActions;
    }

    public BusinessConflictException(String message, Map<String, Object> conflictDetails, List<String> suggestedActions) {
        super(message);
        this.conflictDetails = conflictDetails;
        this.suggestedActions = suggestedActions;
    }
}
