package com.example.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatItem {
    private String label;        // "ACTIVE", "COMPLETED" or "2024-01"
    private Long count;
    private String displayName;  // "Active", "January 2024"
}