package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project partial update data - only non-null fields will be updated")
public class ProjectPatchDTO {
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase letters, digits, and hyphens")
    @Schema(description = "Project code (uppercase letters, digits, and hyphens only)",
            example = "WEB-2024",
            minLength = 3,
            maxLength = 20,
            pattern = "^[A-Z0-9-]+$")
    private String code;

    @Size(min = 3, max = 120)
    @Schema(description = "Project name",
            example = "Website Redesign Project",
            minLength = 3,
            maxLength = 120)
    private String name;

    @Size(max = 2000)
    @Schema(description = "Project description",
            example = "Complete redesign of company website with modern UI/UX",
            maxLength = 2000)
    private String description;

    @Schema(description = "Project status",
            example = "ACTIVE",
            allowableValues = {"PLANNING", "ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED"})
    private ProjectStatus status;

    @Schema(description = "Project start date", example = "2024-01-15")
    private LocalDate startDate;

    @Schema(description = "Project end date (must be on or after start date)", example = "2024-06-30")
    private LocalDate endDate;

    @AssertTrue(message = "End date must be on or after start date")
    private boolean isEndDateValid() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}