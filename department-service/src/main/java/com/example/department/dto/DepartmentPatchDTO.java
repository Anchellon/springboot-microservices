package com.example.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentPatchDTO {
    private String name;        // Optional - null means don't change

    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Code must be 2-10 uppercase letters")
    private String code;        // Optional - null means don't change

    private String description; // Optional - null means don't change
}
