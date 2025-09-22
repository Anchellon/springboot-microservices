package com.example.project.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members", schema = "project",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_employee",
                        columnNames = {"project_id", "employee_id"}
                )
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "employee_id", nullable = false)
    @NotNull
    private Long employeeId;

    @Column(nullable = false, length = 60)
    @NotBlank
    @Size(min = 2, max = 60)
    private String role;

    @Column(name = "allocation_percent", nullable = false)
    @NotNull
    @Min(0)
    @Max(100)
    private Integer allocationPercent;

    @CreationTimestamp
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // Composite unique constraint

}