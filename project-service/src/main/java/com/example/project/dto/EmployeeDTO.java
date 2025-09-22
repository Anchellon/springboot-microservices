package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Employee information")
public class EmployeeDTO {
    @Schema(description = "Employee unique identifier", example = "123")
    private Long id;

    @Schema(description = "Employee first name", example = "John")
    private String firstName;

    @Schema(description = "Employee last name", example = "Doe")
    private String lastName;

    @Schema(description = "Employee email address", example = "john.doe@company.com")
    private String email;
}