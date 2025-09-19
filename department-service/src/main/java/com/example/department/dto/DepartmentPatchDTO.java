// ========================================
// DEPARTMENT PATCH DTO
// ========================================
package com.example.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Department partial update data transfer object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentPatchDTO {
    @Schema(description = "Department name (null means no change)", example = "Engineering")
    private String name;

    @Schema(description = "Department code (2-10 uppercase letters, null means no change)",
            example = "ENG",
            pattern = "^[A-Z]{2,10}$")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Code must be 2-10 uppercase letters")
    private String code;

    @Schema(description = "Department description (null means no change)",
            example = "Software development and engineering team")
    private String description;
}
