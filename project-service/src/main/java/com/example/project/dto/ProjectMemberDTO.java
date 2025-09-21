package com.example.project.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDTO {
    private Long id;
    private Long projectId;
    private Long employeeId;
    private String role;
    private Integer allocationPercent;
    private LocalDateTime assignedAt;
    private EmployeeDTO employee; // For enriched responses
}