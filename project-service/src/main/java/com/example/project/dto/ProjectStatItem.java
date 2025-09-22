package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual statistic item containing label, count and display name")
public class ProjectStatItem {
    @Schema(description = "Raw label value for grouping",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "COMPLETED", "PLANNING", "ON_HOLD", "CANCELLED", "2024-01", "2024-02"})
    private String label;        // "ACTIVE", "COMPLETED" or "2024-01"

    @Schema(description = "Number of projects in this group",
            example = "15")
    private Long count;

    @Schema(description = "Human-readable display name for the label",
            example = "Active Projects")
    private String displayName;  // "Active", "January 2024"
}