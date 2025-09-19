package com.example.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Employee partial update data transfer object")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePatchDTO {
    @Schema(description = "Employee's first name", example = "John")
    private String firstName;

    @Schema(description = "Employee's last name", example = "Doe")
    private String lastName;

    @Schema(description = "Employee's email address", example = "john.doe@example.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Department ID", example = "1")
    private Long departmentId;
}