package com.example.project.domain;

import com.example.project.domain.ProjectStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}