package com.example.project.service;

import com.example.project.domain.ProjectStatus;
import com.example.project.dto.ProjectDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ProjectService {
    Page<ProjectDTO> listProjects(ProjectStatus status, LocalDate from, LocalDate to, String code, String name, Pageable pageable);
}
