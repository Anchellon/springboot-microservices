// ========================================
// EMPLOYEE DTO (FOR DEPARTMENT SERVICE)
// ========================================
package com.example.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Employee data transfer object (used in department context)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    @Schema(description = "Employee ID", example = "1")
    private Long id;

    @Schema(description = "Employee's first name", example = "John")
    private String firstName;

    @Schema(description = "Employee's last name", example = "Doe")
    private String lastName;

    @Schema(description = "Employee's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Department ID", example = "1")
    private Long departmentId;
}