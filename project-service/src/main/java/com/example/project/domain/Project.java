package com.example.project.domain;

import com.example.project.domain.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects",schema = "project")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 20)
    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase letters, digits, and hyphens")
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    @NotBlank
    @Size(min = 3, max = 120)
    private String name;

    @Column(name = "description", length = 2000)
    @Size(max = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    private ProjectStatus status;

    @Column(name = "start_date", nullable = false)
    @NotNull
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;


    // Custom validation method
    @AssertTrue(message = "End date must be on or after start date")
    private boolean isEndDateValid() {
        return endDate == null || !endDate.isBefore(startDate);
    }


    // Optimized lazy relationship with fallback protection
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 25) // Protection against accidental N+1
    @JsonIgnore // Prevent accidental serialization
    private List<ProjectMember> members = new ArrayList<>();





    // Controlled access with warning
    public List<ProjectMember> getMembers() {
        log.debug("PERFORMANCE WARNING: Direct member access on Project {} - consider using repository queries instead", getId());
        return members;
    }

    // Helper methods for managing members
    public void addMember(ProjectMember member) {
        members.add(member);
        member.setProject(this);
    }

    public void removeMember(ProjectMember member) {
        members.remove(member);
        member.setProject(null);
    }

    public void clearMembers() {
        members.forEach(member -> member.setProject(null));
        members.clear();
    }
}