package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPatchDTO {
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase letters, digits, and hyphens")
    private String code;

    @Size(min = 3, max = 120)
    private String name;

    @Size(max = 2000)
    private String description;

    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    @AssertTrue(message = "End date must be on or after start date")
    private boolean isEndDateValid() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}