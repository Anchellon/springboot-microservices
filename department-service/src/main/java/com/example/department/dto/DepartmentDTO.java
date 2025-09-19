// ========================================
// DEPARTMENT DTO
// ========================================
package com.example.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Department data transfer object for create, read, and update operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    @Schema(description = "Department ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Department name", example = "Engineering", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Department name is required")
    private String name;

    @Schema(description = "Department code (2-10 uppercase letters)",
            example = "ENG",
            pattern = "^[A-Z]{2,10}$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Department code is required")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Code must be 2-10 uppercase letters")
    private String code;

    @Schema(description = "Department description", example = "Software development and engineering team")
    private String description;
}