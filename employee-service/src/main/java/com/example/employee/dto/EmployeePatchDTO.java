package com.example.employee.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePatchDTO {
    private String firstName;      // Optional - null means "don't change"
    private String lastName;       // Optional - null means "don't change"

    @Email(message = "Invalid email format") // Validate format IF provided
    private String email;          // Optional - null means "don't change"

    private Long departmentId;     // Optional - null means "don't change"
}