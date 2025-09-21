package com.example.project.service.impl;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectMember;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.exception.*;
import com.example.project.mapper.ProjectMapper;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import com.example.project.repo.ProjectStatProjection;
import com.example.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final EmployeeValidationService employeeValidationService;

    @Override
    public Page<ProjectDTO> listProjects(ProjectStatus status, LocalDate from, LocalDate to,
                                         String code, String name, Pageable pageable) {

        log.debug("Service: Fetching projects with filters");

        Page<Project> projects = projectRepository.findProjectsWithFilters(
                status, from, to, code, name, pageable);

        log.debug("Found {} projects", projects.getTotalElements());

        return projects.map(projectMapper::toDTO);
    }

    @Override
    public ProjectDTO getProjectById(Long id) {
        log.debug("Service: Fetching project with id: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        log.debug("Found project: {}", project.getCode());

        return projectMapper.toDTO(project);
    }

    @Transactional
    @Override
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        log.debug("Service: Creating project with code: {}", projectDTO.getCode());

        // Convert DTO to Entity (mapper will ignore id and members)
        Project project = projectMapper.toEntity(projectDTO);

        // Save the project
        Project savedProject = projectRepository.save(project);

        log.info("Successfully created project with id: {} and code: {}",
                savedProject.getId(), savedProject.getCode());

        return projectMapper.toDTO(savedProject);
    }

    @Override
    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        log.debug("Service: Updating project with id: {}", id);

        // 1. Check if project exists
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        log.debug("Found existing project: {}", existingProject.getCode());

        // 2. Check if code is being changed and if new code conflicts
        if (!existingProject.getCode().equals(projectDTO.getCode())) {
            log.debug("Code change detected from {} to {}", existingProject.getCode(), projectDTO.getCode());

            // Check if new code already exists on another project
            boolean codeExists = projectRepository.existsByCodeAndIdNot(projectDTO.getCode(), id);
            if (codeExists) {
                throw new BusinessConflictException("Project code already exists and cannot be changed to duplicate value");
            }
        }

        // 3. Update all fields (full replacement)
        existingProject.setCode(projectDTO.getCode());
        existingProject.setName(projectDTO.getName());
        existingProject.setDescription(projectDTO.getDescription());
        existingProject.setStatus(projectDTO.getStatus());
        existingProject.setStartDate(projectDTO.getStartDate());
        existingProject.setEndDate(projectDTO.getEndDate());

        // 4. Save updated project
        Project updatedProject = projectRepository.save(existingProject);

        log.info("Successfully updated project with id: {} and code: {}",
                updatedProject.getId(), updatedProject.getCode());

        return projectMapper.toDTO(updatedProject);
    }

    @Transactional
    @Override
    public ProjectDTO patchProject(Long id, ProjectPatchDTO patchDTO) {
        log.debug("Service: Patching project with id: {}", id);

        // 1. Check if project exists
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        log.debug("Found existing project: {}", existingProject.getCode());

        // 2. Track what fields are being updated for logging
        StringBuilder updatedFields = new StringBuilder();

        // 3. Apply partial updates (only non-null fields)
        if (patchDTO.getCode() != null) {
            // Check for code conflicts if code is being changed
            if (!existingProject.getCode().equals(patchDTO.getCode())) {
                log.debug("Code change detected from {} to {}", existingProject.getCode(), patchDTO.getCode());

                boolean codeExists = projectRepository.existsByCodeAndIdNot(patchDTO.getCode(), id);
                if (codeExists) {
                    throw new BusinessConflictException("Project code already exists and cannot be changed to duplicate value");
                }

                existingProject.setCode(patchDTO.getCode());
                updatedFields.append("code ");
            }
        }

        if (patchDTO.getName() != null) {
            existingProject.setName(patchDTO.getName());
            updatedFields.append("name ");
        }

        if (patchDTO.getDescription() != null) {
            existingProject.setDescription(patchDTO.getDescription());
            updatedFields.append("description ");
        }

        if (patchDTO.getStatus() != null) {
            existingProject.setStatus(patchDTO.getStatus());
            updatedFields.append("status ");
        }

        if (patchDTO.getStartDate() != null) {
            existingProject.setStartDate(patchDTO.getStartDate());
            updatedFields.append("startDate ");
        }

        if (patchDTO.getEndDate() != null) {
            existingProject.setEndDate(patchDTO.getEndDate());
            updatedFields.append("endDate ");
        }

        // 4. Save updated project
        Project patchedProject = projectRepository.save(existingProject);

        log.info("Successfully patched project with id: {} and code: {}. Updated fields: [{}]",
                patchedProject.getId(), patchedProject.getCode(), updatedFields.toString().trim());

        return projectMapper.toDTO(patchedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectMemberDTO> getProjectMembers(Long projectId, boolean enrich, Pageable pageable) {
        log.debug("Getting members for project {} with enrich={}, page={}, size={}",
                projectId, enrich, pageable.getPageNumber(), pageable.getPageSize());

        // 1. Verify project exists (lightweight check)
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        // 2. Get paginated members from database - FIXED: Use correct method name
        Page<ProjectMember> memberPage = projectMemberRepository.findByProject_Id(projectId, pageable);

        if (!enrich || memberPage.isEmpty()) {
            // Return basic member info only - no external service calls
            log.debug("Returning {} basic members (no enrichment)", memberPage.getNumberOfElements());
            return memberPage.map(projectMapper::memberToDTO);
        }

        // 3. Enrich with employee details
        log.debug("Enriching {} members with employee details", memberPage.getNumberOfElements());

        // Extract employee IDs from current page only (e.g., 20-50 employees max)
        Set<Long> employeeIds = memberPage.getContent().stream()
                .map(ProjectMember::getEmployeeId)
                .collect(Collectors.toSet());

        // 4. Batch fetch employee details for current page
        // This calls your Employee Service: GET /api/v1/employees/{id}?enrichWithDepartment=false
        // We use enrichWithDepartment=false since we only need { id, firstName, lastName, email }
        List<EmployeeDTO> employees = employeeValidationService.getEmployeesBasic(employeeIds);

        // 5. Create lookup map for efficient access
        Map<Long, EmployeeDTO> employeeMap = employees.stream()
                .collect(Collectors.toMap(EmployeeDTO::getId, employee -> employee));

        // 6. Combine member data with employee data
        return memberPage.map(member -> {
            ProjectMemberDTO dto = projectMapper.memberToDTO(member);
            EmployeeDTO employeeData = employeeMap.get(member.getEmployeeId());
            dto.setEmployee(employeeData); // This will have id, firstName, lastName, email
            return dto;
        });
    }

    @Transactional
    @Override
    public void deleteProject(Long id) {
        log.debug("Service: Deleting project with id: {}", id);

        // Check existence without loading entity
        if (!projectRepository.existsById(id)) {
            throw new ProjectNotFoundException(id);
        }

        // Check member count using efficient query - FIXED: Use correct method name
        long memberCount = projectMemberRepository.countByProject_Id(id);

        if (memberCount > 0) {
            // Get minimal data for conflict response
            List<Long> memberIds = projectMemberRepository.findEmployeeIdsByProjectId(id);

            // FIXED: Handle Optional<String> return type from findCodeById
            String projectCode = projectRepository.findCodeById(id)
                    .orElseThrow(() -> new ProjectNotFoundException(id));

            List<String> suggestedActions = List.of(
                    "Remove all project members first using DELETE /api/v1/projects/" + id + "/members/{employeeId}",
                    "Or use GET /api/v1/projects/" + id + "/members to see all members that need to be removed"
            );

            Map<String, Object> conflictDetails = Map.of(
                    "projectId", id,
                    "projectCode", projectCode,
                    "memberCount", memberCount,
                    "memberIds", memberIds
            );

            throw new BusinessConflictException(
                    "Cannot delete project with active members",
                    conflictDetails,
                    suggestedActions
            );
        }

        projectRepository.deleteById(id);
        log.info("Successfully deleted project with id: {}", id);
    }

    // Add this method to your ProjectServiceImpl class

    @Transactional
    @Override
    public List<ProjectMemberDTO> addProjectMembers(Long projectId, List<ProjectMemberDTO> memberRequests) {
        log.debug("Service: Adding {} members to project {}", memberRequests.size(), projectId);

        // 1. Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        // 2. Extract and validate unique employee IDs from request
        Set<Long> requestedEmployeeIds = memberRequests.stream()
                .map(ProjectMemberDTO::getEmployeeId)
                .collect(Collectors.toSet());

        // Check for duplicate employee IDs in the request itself
        if (requestedEmployeeIds.size() != memberRequests.size()) {
            List<Long> duplicateIds = memberRequests.stream()
                    .collect(Collectors.groupingBy(ProjectMemberDTO::getEmployeeId, Collectors.counting()))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .toList();

            throw new BusinessConflictException(
                    "Duplicate employee IDs in request: " + duplicateIds
            );
        }

        log.debug("Validating {} unique employees", requestedEmployeeIds.size());

        // 3. Validate all employees exist (batch validation)
        try {
            List<EmployeeDTO> validatedEmployees = employeeValidationService.validateEmployeesExist(requestedEmployeeIds);
            log.debug("All {} employees validated successfully", validatedEmployees.size());
        } catch (ExternalServiceNotFoundException e) {
            log.error("Employee validation failed - employee not found: {}", e.getMessage());
            throw new BusinessConflictException("One or more employees not found: " + e.getMessage());
        } catch (ExternalServiceException e) {
            log.error("Employee service unavailable during validation: {}", e.getMessage());
            throw e; // Re-throw service exception
        }

        // 4. Check for existing project memberships
        List<Long> existingMemberIds = requestedEmployeeIds.stream()
                .filter(employeeId -> projectMemberRepository.existsByProject_IdAndEmployeeId(projectId, employeeId))
                .toList();

        if (!existingMemberIds.isEmpty()) {
            Map<String, Object> conflictDetails = Map.of(
                    "projectId", projectId,
                    "projectCode", project.getCode(),
                    "existingMemberEmployeeIds", existingMemberIds
            );

            throw new BusinessConflictException(
                    "One or more employees are already project members",
                    conflictDetails,
                    List.of("Remove duplicate employees from request or use PATCH to update existing members")
            );
        }

        // 5. Create new ProjectMember entities
        List<ProjectMember> newMembers = memberRequests.stream()
                .map(request -> {
                    ProjectMember member = new ProjectMember();
                    member.setProject(project);
                    member.setEmployeeId(request.getEmployeeId());
                    member.setRole(request.getRole());
                    member.setAllocationPercent(request.getAllocationPercent());
                    // assignedAt will be auto-set by @CreationTimestamp
                    return member;
                })
                .collect(Collectors.toList());

        // 6. Save all members in single transaction
        List<ProjectMember> savedMembers = projectMemberRepository.saveAll(newMembers);

        log.info("Successfully added {} members to project {} ({})",
                savedMembers.size(), projectId, project.getCode());

        // 7. Convert to DTOs and return
        return savedMembers.stream()
                .map(projectMapper::memberToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    @Override
    public void removeProjectMember(Long projectId, Long employeeId) {
        log.debug("Service: Removing employee {} from project {}", employeeId, projectId);

        // 1. Verify project exists (lightweight check)
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        // 2. Verify member exists in this project
        if (!projectMemberRepository.existsByProject_IdAndEmployeeId(projectId, employeeId)) {
            throw new ProjectMemberNotFoundException(projectId, employeeId);
        }

        // 3. Delete the membership
        projectMemberRepository.deleteByProject_IdAndEmployeeId(projectId, employeeId);

        log.info("Successfully removed employee {} from project {}", employeeId, projectId);
    }


    @Override
    @Transactional(readOnly = true)
    public ProjectStatsDTO getProjectStats(String groupBy) {
        log.debug("Service: Getting project stats grouped by: {}", groupBy);

        if ("status".equals(groupBy)) {
            return getStatsByStatus();
        } else if ("month".equals(groupBy)) {
            return getStatsByMonth();
        } else {
            throw new IllegalArgumentException("groupBy must be 'status' or 'month'");
        }
    }

    private ProjectStatsDTO getStatsByStatus() {
        log.debug("Getting project stats by status");

        List<ProjectStatProjection> results = projectRepository.countByStatus();

        List<ProjectStatItem> stats = results.stream()
                .map(result -> ProjectStatItem.builder()
                        .label(result.getLabel())
                        .count(result.getCount())
                        .displayName(formatStatusDisplayName(result.getLabel()))
                        .build())
                .collect(Collectors.toList());

        log.debug("Found {} status groups", stats.size());

        return ProjectStatsDTO.builder()
                .groupBy("status")
                .stats(stats)
                .build();
    }

    private ProjectStatsDTO getStatsByMonth() {
        log.debug("Getting project stats by start month");

        List<ProjectStatProjection> results = projectRepository.countByStartMonth();

        List<ProjectStatItem> stats = results.stream()
                .map(result -> ProjectStatItem.builder()
                        .label(result.getLabel())
                        .count(result.getCount())
                        .displayName(formatMonthDisplayName(result.getLabel()))
                        .build())
                .collect(Collectors.toList());

        log.debug("Found {} month groups", stats.size());

        return ProjectStatsDTO.builder()
                .groupBy("month")
                .stats(stats)
                .build();
    }

    private String formatStatusDisplayName(String status) {
        if (status == null) return "Unknown";

        // Convert enum to display format: "ACTIVE" -> "Active"
        return Character.toUpperCase(status.charAt(0)) + status.substring(1).toLowerCase();
    }

    private String formatMonthDisplayName(String monthLabel) {
        if (monthLabel == null) return "Unknown";

        try {
            // Parse "2024-09" format
            String[] parts = monthLabel.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            String[] monthNames = {
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
            };

            return monthNames[month - 1] + " " + year;
        } catch (Exception e) {
            log.warn("Failed to format month display name: {}", monthLabel);
            return monthLabel; // Fallback to original label
        }
    }
}