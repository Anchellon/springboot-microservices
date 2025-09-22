package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project information with validation constraints")
public class ProjectDTO {
    @Schema(description = "Project unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase letters, digits, and hyphens")
    @Schema(description = "Project code (uppercase letters, digits, and hyphens only)",
            example = "WEB-2024",
            minLength = 3,
            maxLength = 20,
            pattern = "^[A-Z0-9-]+$")
    private String code;

    @NotBlank
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

    @NotNull
    @Schema(description = "Current project status",
            example = "ACTIVE",
            allowableValues = {"PLANNING", "ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED"})
    private ProjectStatus status;

    @NotNull
    @Schema(description = "Project start date", example = "2024-01-15")
    private LocalDate startDate;

    @Schema(description = "Project end date (must be on or after start date)", example = "2024-06-30")
    private LocalDate endDate;

    @Schema(description = "List of project members", accessMode = Schema.AccessMode.READ_ONLY)
    private List<ProjectMemberDTO> members;

    @Schema(description = "Project creation timestamp",
            example = "2024-01-10T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Project last update timestamp",
            example = "2024-01-20T14:45:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    // Custom validation method
    @AssertTrue(message = "End date must be on or after start date")
    private boolean isEndDateValid() {
        return endDate == null || !endDate.isBefore(startDate);
    }
}