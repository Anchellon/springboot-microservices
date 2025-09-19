package com.example.employee.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "Employee data transfer object")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDTO {
    @Schema(description = "Employee ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Employee's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "firstName is required")
    @Size(max = 120)
    private String firstName;

    @Schema(description = "Employee's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "lastName is required")
    @Size(max = 120)
    private String lastName;

    @Schema(description = "Employee's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 200)
    private String email;

    @Schema(description = "Department ID", example = "1")
    private Long departmentId;

    @Schema(description = "Department details (included when enriched)")
    private DepartmentDTO department;
}