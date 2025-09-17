package com.example.employee.exception;

import java.util.HashMap;
import java.util.Map;

public class BusinessRuleViolationException extends RuntimeException {
    private final String rule;
    private final Map<String, Object> context;

    public BusinessRuleViolationException(String rule, String message) {
        super(message);
        this.rule = rule;
        this.context = new HashMap<>();
    }

    public BusinessRuleViolationException(String rule, String message, Map<String, Object> context) {
        super(message);
        this.rule = rule;
        this.context = context != null ? context : new HashMap<>();
    }

    public String getRule() { return rule; }
    public Map<String, Object> getContext() { return context; }
}
