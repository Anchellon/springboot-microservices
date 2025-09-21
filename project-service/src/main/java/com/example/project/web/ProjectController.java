package com.example.project.web;

import com.example.project.dto.*;
import com.example.project.domain.ProjectStatus;
import com.example.project.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<Page<ProjectDTO>> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        log.debug("Listing projects with filters - status: {}, from: {}, to: {}, code: {}, name: {}",
                status, from, to, code, name);

        Page<ProjectDTO> projects = projectService.listProjects(status, from, to, code, name, pageable);
        return ResponseEntity.ok(projects);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        log.debug("Getting project with id: {}", id);

        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDTO projectDTO) {

        log.debug("Updating project with id: {} and code: {}", id, projectDTO.getCode());

        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);

        log.info("Successfully updated project with id: {}", updatedProject.getId());

        return ResponseEntity.ok(updatedProject);
    }
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDTO> patchProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectPatchDTO patchDTO) {

        log.debug("Patching project with id: {}", id);

        ProjectDTO patchedProject = projectService.patchProject(id, patchDTO);

        log.info("Successfully patched project with id: {}", patchedProject.getId());

        return ResponseEntity.ok(patchedProject);
    }
    @GetMapping("/{id}/members")
    public ResponseEntity<Page<ProjectMemberDTO>> getProjectMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean enrich,
            @PageableDefault(size = 50, sort = "assignedAt") Pageable pageable) {

        log.debug("Getting members for project {} with enrich={}", id, enrich);

        Page<ProjectMemberDTO> members = projectService.getProjectMembers(id, enrich, pageable);
        return ResponseEntity.ok(members);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.debug("Deleting project with id: {}", id);

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
    @PostMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberDTO>> addProjectMembers(
            @PathVariable Long projectId,
            @RequestBody @Valid List<ProjectMemberDTO> memberRequests) {

        log.info("REST: Adding {} members to project {}", memberRequests.size(), projectId);

        // Validate request is not empty
        if (memberRequests == null || memberRequests.isEmpty()) {
            throw new IllegalArgumentException("Member requests cannot be empty");
        }

        // Log the employee IDs being added for debugging
        List<Long> employeeIds = memberRequests.stream()
                .map(ProjectMemberDTO::getEmployeeId)
                .collect(Collectors.toList());
        log.debug("Employee IDs to add: {}", employeeIds);

        // Call service to add members
        List<ProjectMemberDTO> createdMembers = projectService.addProjectMembers(projectId, memberRequests);

        log.info("REST: Successfully added {} members to project {}", createdMembers.size(), projectId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdMembers);
    }
    /**
     * Remove a member from a project
     * DELETE /api/v1/projects/{projectId}/members/{employeeId}
     */
    @DeleteMapping("/{projectId}/members/{employeeId}")
    public ResponseEntity<Void> removeProjectMember(
            @PathVariable Long projectId,
            @PathVariable Long employeeId) {

        log.info("REST: Removing employee {} from project {}", employeeId, projectId);

        // Call service to remove member
        projectService.removeProjectMember(projectId, employeeId);

        log.info("REST: Successfully removed employee {} from project {}", employeeId, projectId);

        return ResponseEntity.noContent().build(); // 204 No Content
    }
    /**
     * Get project statistics
     * GET /api/v1/projects/stats?groupBy=status
     * GET /api/v1/projects/stats?groupBy=month
     */
    @GetMapping("/stats")
    public ResponseEntity<ProjectStatsDTO> getProjectStats(
            @RequestParam(defaultValue = "status") String groupBy) {

        log.info("REST: Getting project stats grouped by: {}", groupBy);

        // Validate groupBy parameter
        if (!List.of("status", "month").contains(groupBy)) {
            throw new IllegalArgumentException("groupBy must be 'status' or 'month'");
        }

        ProjectStatsDTO stats = projectService.getProjectStats(groupBy);

        log.info("REST: Successfully retrieved project stats for groupBy: {} (found {} groups)",
                groupBy, stats.getStats().size());

        return ResponseEntity.ok(stats);
    }
}
