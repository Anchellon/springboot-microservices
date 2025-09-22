package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Project member information with role and allocation details")
public class ProjectMemberDTO {
    @Schema(description = "Project member unique identifier",
            example = "456",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id; // Will be null for POST requests

    @Schema(description = "Project ID (ignored in requests, comes from path parameter)",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long projectId; // Will be ignored (comes from path)

    @NotNull(message = "Employee ID is required")
    @Schema(description = "Employee unique identifier", example = "123", required = true)
    private Long employeeId;

    @NotBlank(message = "Role is required")
    @Size(min = 2, max = 60, message = "Role must be between 2 and 60 characters")
    @Schema(description = "Employee role in the project",
            example = "Frontend Developer",
            minLength = 2,
            maxLength = 60,
            required = true)
    private String role;

    @NotNull(message = "Allocation percent is required")
    @Min(value = 0, message = "Allocation percent must be at least 0")
    @Max(value = 100, message = "Allocation percent must be at most 100")
    @Schema(description = "Percentage of employee's time allocated to this project",
            example = "75",
            minimum = "0",
            maximum = "100",
            required = true)
    private Integer allocationPercent;

    @Schema(description = "Timestamp when employee was assigned to project",
            example = "2024-01-15T09:30:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime assignedAt; // Will be null for POST

    @Schema(description = "Employee details (populated when enriched)",
            accessMode = Schema.AccessMode.READ_ONLY)
    private EmployeeDTO employee; // Will be null for POST
}