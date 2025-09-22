package com.example.project.web;

import com.example.project.dto.*;
import com.example.project.domain.ProjectStatus;
import com.example.project.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
    @Operation(summary = "Get all projects with pagination and filtering",
            description = "Retrieve a paginated list of projects with optional filtering by status, date range, code, and name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved projects"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<Page<ProjectDTO>> listProjects(
            @Parameter(description = "Filter by project status", example = "ACTIVE")
            @RequestParam(required = false) ProjectStatus status,
            @Parameter(description = "Filter by start date from (inclusive)", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Filter by start date to (inclusive)", example = "2024-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filter by project code containing text", example = "WEB")
            @RequestParam(required = false) String code,
            @Parameter(description = "Filter by project name containing text", example = "Website")
            @RequestParam(required = false) String name,
            @Parameter(description = "Pagination and sorting parameters (page, size, sort)",
                    example = "page=0&size=20&sort=id,asc")
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        log.debug("Listing projects with filters - status: {}, from: {}, to: {}, code: {}, name: {}",
                status, from, to, code, name);

        Page<ProjectDTO> projects = projectService.listProjects(status, from, to, code, name, pageable);
        return ResponseEntity.ok(projects);
    }
    @PostMapping
    @Operation(summary = "Create new project", description = "Create a new project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid project data"),
            @ApiResponse(responseCode = "409", description = "Project code already exists")
    })
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody ProjectDTO projectDTO) {

        log.debug("Creating project with code: {}", projectDTO.getCode());

        ProjectDTO createdProject = projectService.createProject(projectDTO);

        log.info("Successfully created project with id: {}", createdProject.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Retrieve a specific project by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved project"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectDTO> getProjectById(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("Getting project with id: {}", id);

        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project completely", description = "Replace all project data with new values")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "400", description = "Invalid project data"),
            @ApiResponse(responseCode = "409", description = "Project code conflicts with existing project")
    })
    public ResponseEntity<ProjectDTO> updateProject(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated project data", required = true)
            @Valid @RequestBody ProjectDTO projectDTO) {

        log.debug("Updating project with id: {} and code: {}", id, projectDTO.getCode());

        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);

        log.info("Successfully updated project with id: {}", updatedProject.getId());

        return ResponseEntity.ok(updatedProject);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update project", description = "Update specific fields of a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "409", description = "Project code conflicts")
    })
    public ResponseEntity<ProjectDTO> patchProject(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Fields to update (null values are ignored)", required = true)
            @Valid @RequestBody ProjectPatchDTO patchDTO){

        log.debug("Patching project with id: {}", id);

        ProjectDTO patchedProject = projectService.patchProject(id, patchDTO);

        log.info("Successfully patched project with id: {}", patchedProject.getId());

        return ResponseEntity.ok(patchedProject);
    }
    @GetMapping("/{id}/members")
    @Operation(summary = "Get project members",
            description = "Retrieve paginated list of project members with optional enrichment of employee details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved project members"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<ProjectMemberDTO>> getProjectMembers(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Include enriched employee details in response", example = "false")
            @RequestParam(defaultValue = "false") boolean enrich,
            @Parameter(description = "Pagination and sorting parameters (page, size, sort)",
                    example = "page=0&size=50&sort=assignedAt,asc")
            @PageableDefault(size = 50, sort = "assignedAt") Pageable pageable){

        log.debug("Getting members for project {} with enrich={}", id, enrich);

        Page<ProjectMemberDTO> members = projectService.getProjectMembers(id, enrich, pageable);
        return ResponseEntity.ok(members);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Delete a project by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Project has active members or dependencies and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("Deleting project with id: {}", id);

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }


    @PostMapping("/{projectId}/members")
    @Operation(summary = "Add members to project",
            description = "Add one or more employees as members to a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Members added successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "400", description = "Invalid member data or empty request"),
            @ApiResponse(responseCode = "409", description = "One or more employees are already members of this project",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<ProjectMemberDTO>> addProjectMembers(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "List of members to add to the project", required = true)
            @RequestBody @Valid List<ProjectMemberDTO> memberRequests){

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
    @Operation(summary = "Remove member from project",
            description = "Remove an employee from a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found or employee is not a member of this project")
    })
    public ResponseEntity<Void> removeProjectMember(
            @Parameter(description = "Project ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "Employee ID", required = true, example = "123")
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
    @Operation(summary = "Get project statistics",
            description = "Retrieve project statistics grouped by status or month")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved project statistics"),
            @ApiResponse(responseCode = "400", description = "Invalid groupBy parameter")
    })
    public ResponseEntity<ProjectStatsDTO> getProjectStats(
            @Parameter(description = "Group statistics by 'status' or 'month'",
                    example = "status",
                    schema = @Schema(allowableValues = {"status", "month"}))
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
