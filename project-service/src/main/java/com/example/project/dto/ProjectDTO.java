package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
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
public class ProjectDTO {
    private Long id;

    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase letters, digits, and hyphens")
    private String code;

    @NotBlank
    @Size(min = 3, max = 120)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    private ProjectStatus status;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private List<ProjectMemberDTO> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Custom validation method
    @AssertTrue(message = "End date must be on or after start date")
    private boolean isEndDateValid() {
        return endDate == null || !endDate.isBefore(startDate);
    }
}