package com.example.project.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDTO {
    private Long id; // Will be null for POST requests
    private Long projectId; // Will be ignored (comes from path)

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Role is required")
    @Size(min = 2, max = 60, message = "Role must be between 2 and 60 characters")
    private String role;

    @NotNull(message = "Allocation percent is required")
    @Min(value = 0, message = "Allocation percent must be at least 0")
    @Max(value = 100, message = "Allocation percent must be at most 100")
    private Integer allocationPercent;

    private LocalDateTime assignedAt; // Will be null for POST
    private EmployeeDTO employee; // Will be null for POST
}