package com.example.department.exception;

import lombok.Getter;

@Getter
public class DepartmentInUseException extends RuntimeException {
    private final Long departmentId;
    private final long employeeCount;
    private final String departmentName;

    public DepartmentInUseException(Long departmentId, String departmentName, long employeeCount) {
        super(String.format("Cannot delete department '%s' (ID: %d): %d employees are still assigned",
                departmentName, departmentId, employeeCount));
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.employeeCount = employeeCount;
    }
}
