// ========================================
// DEPARTMENT EMPLOYEES DTO
// ========================================
package com.example.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Schema(description = "Department information combined with paginated employee list")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEmployeesDTO {
    @Schema(description = "Department information")
    private DepartmentDTO department;

    @Schema(description = "Paginated list of employees in this department")
    private Page<EmployeeDTO> employees;

    @Schema(description = "Total number of employees in this department", example = "25")
    private long totalEmployees;

    @Schema(description = "Summary information", example = "Engineering department has 25 employees")
    private String summary;
}

