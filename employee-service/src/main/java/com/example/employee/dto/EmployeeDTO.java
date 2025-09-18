package com.example.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class    EmployeeDTO {
    private Long id;

    @NotBlank(message = "firstName is required")
    @Size(max = 120)
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 120)
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 200)
    private String email;

    private Long departmentId;
    private DepartmentDTO department; // response enrichment
}
