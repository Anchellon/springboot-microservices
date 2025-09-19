package com.example.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Schema(description = "Employee statistics data transfer object")
@Data
public class EmployeeStatsDTO {
    @Schema(description = "Total number of employees", example = "150")
    private long totalEmployees;

    @Schema(description = "Employee count by department ID")
    private Map<Long, Long> employeesByDepartment;

    @Schema(description = "Employee count by department name")
    private Map<String, Long> employeesByDepartmentName;

    @Schema(description = "Number of departments with employees", example = "5")
    private long departmentsWithEmployees;

    @Schema(description = "Average employees per department", example = "30.0")
    private double averageEmployeesPerDepartment;
}