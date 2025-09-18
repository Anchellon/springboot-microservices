package com.example.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Single DTO for GET, POST, and PUT operations
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private Long id; // Not required for POST/PUT (comes from URL for PUT)

    @NotBlank(message = "Department name is required")
    private String name;

    @NotBlank(message = "Department code is required")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Code must be 2-10 uppercase letters")
    private String code;

    private String description; // Optional for all operations
}