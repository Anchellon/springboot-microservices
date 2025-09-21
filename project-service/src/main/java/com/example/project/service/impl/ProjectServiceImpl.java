package com.example.project.service.impl;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.ProjectDTO;
import com.example.project.repo.ProjectRepository;
import com.example.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public Page<ProjectDTO> listProjects(ProjectStatus status, LocalDate from, LocalDate to,
                                         String code, String name, Pageable pageable) {

        log.debug("Service: Fetching projects with filters");

        Page<Project> projects = projectRepository.findProjectsWithFilters(
                status, from, to, code, name, pageable);

        log.debug("Found {} projects", projects.getTotalElements());

        return projects.map(projectMapper::toDTO);
    }
}