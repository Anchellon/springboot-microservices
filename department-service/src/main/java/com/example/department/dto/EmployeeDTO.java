package com.example.department.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for employee data from Employee service
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long departmentId;
    // Note: No department object to avoid circular references
}
