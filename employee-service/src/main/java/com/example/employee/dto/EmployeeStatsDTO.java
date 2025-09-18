package com.example.employee.dto;

import lombok.Data;
import java.util.Map;

@Data
public class EmployeeStatsDTO {

    private long totalEmployees;                    // Total count across all departments
    private Map<Long, Long> employeesByDepartment;  // Map: departmentId → employee count
    private Map<String, Long> employeesByDepartmentName; // Map: department name → count

    // Additional metrics can be added here
    private long departmentsWithEmployees;          // Number of departments that have employees
    private double averageEmployeesPerDepartment;   // Average employees per department
}