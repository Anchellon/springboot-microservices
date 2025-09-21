package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProjectStatsDTO {
    private String groupBy;
    private List<ProjectStatItem> stats;
}
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatItem {
    private String category;
    private Long count;
}