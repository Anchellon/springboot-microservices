package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Schema(description = "Project statistics grouped by specified criteria")
public class ProjectStatsDTO {
    @Schema(description = "The grouping method used for statistics",
            example = "status",
            allowableValues = {"status", "month"})
    private String groupBy;

    @Schema(description = "List of statistical items for each group")
    private List<ProjectStatItem> stats;
}