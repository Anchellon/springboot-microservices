package com.example.project.service;

import com.example.project.domain.ProjectStatus;
import com.example.project.dto.ProjectDTO;
import com.example.project.dto.ProjectMemberDTO;
import com.example.project.dto.ProjectPatchDTO;
import com.example.project.dto.ProjectStatsDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface ProjectService {
    Page<ProjectDTO> listProjects(ProjectStatus status, LocalDate from, LocalDate to, String code, String name, Pageable pageable);

    ProjectDTO getProjectById(Long id);

    @Transactional
    ProjectDTO createProject(ProjectDTO projectDTO);

    ProjectDTO updateProject(Long id, @Valid ProjectDTO projectDTO);

    @Transactional
    ProjectDTO patchProject(Long id, ProjectPatchDTO patchDTO);

    Page<ProjectMemberDTO> getProjectMembers(Long id, boolean enrich, Pageable pageable);

    @Transactional
    void deleteProject(Long id);

    @Transactional
    List<ProjectMemberDTO> addProjectMembers(Long projectId, List<ProjectMemberDTO> memberRequests);

    void removeProjectMember(Long projectId, Long employeeId);

    ProjectStatsDTO getProjectStats(String groupBy);
}
