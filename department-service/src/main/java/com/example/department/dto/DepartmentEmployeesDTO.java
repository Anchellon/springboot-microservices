
// ========================================
// COMPOSED RESPONSE DTO
// ========================================
package com.example.department.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEmployeesDTO {
    // Department information
    private DepartmentDTO department;

    // Employee pagination info
    private Page<EmployeeDTO> employees;

    // Summary information
    private long totalEmployees;
    private String summary;
}
