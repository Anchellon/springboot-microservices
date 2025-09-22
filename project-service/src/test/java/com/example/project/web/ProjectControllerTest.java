package com.example.project.web;

import com.example.project.dto.*;
import com.example.project.domain.ProjectStatus;
import com.example.project.exception.*;
import com.example.project.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@DisplayName("ProjectController Web Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectDTO sampleProject;
    private ProjectPatchDTO samplePatchDTO;
    private ProjectMemberDTO sampleMember;

    @BeforeEach
    void setUp() {
        sampleProject = ProjectDTO.builder()
                .id(1L)
                .code("TEST-2024")
                .name("Test Project")
                .description("A test project")
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 15))
                .endDate(LocalDate.of(2024, 6, 30))
                .createdAt(LocalDateTime.of(2024, 1, 10, 10, 30))
                .updatedAt(LocalDateTime.of(2024, 1, 20, 14, 45))
                .build();

        samplePatchDTO = ProjectPatchDTO.builder()
                .name("Updated Project Name")
                .description("Updated description")
                .build();

        sampleMember = ProjectMemberDTO.builder()
                .id(1L)
                .projectId(1L)
                .employeeId(123L)
                .role("Developer")
                .allocationPercent(75)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/projects - List Projects")
    class ListProjectsTests {

        @Test
        @DisplayName("Should return paginated projects successfully")
        void shouldReturnPaginatedProjects() throws Exception {
            // Given
            PageImpl<ProjectDTO> projectPage = new PageImpl<>(
                    List.of(sampleProject),
                    PageRequest.of(0, 20),
                    1
            );
            when(projectService.listProjects(any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(projectPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].code").value("TEST-2024"))
                    .andExpect(jsonPath("$.content[0].name").value("Test Project"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.size").value(20));

            verify(projectService).listProjects(any(), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter projects by status")
        void shouldFilterProjectsByStatus() throws Exception {
            // Given
            PageImpl<ProjectDTO> projectPage = new PageImpl<>(List.of(sampleProject));
            when(projectService.listProjects(eq(ProjectStatus.ACTIVE), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(projectPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

            verify(projectService).listProjects(eq(ProjectStatus.ACTIVE), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter projects by date range")
        void shouldFilterProjectsByDateRange() throws Exception {
            // Given
            PageImpl<ProjectDTO> projectPage = new PageImpl<>(List.of(sampleProject));
            when(projectService.listProjects(any(), any(LocalDate.class), any(LocalDate.class), any(), any(), any(Pageable.class)))
                    .thenReturn(projectPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects")
                            .param("from", "2024-01-01")
                            .param("to", "2024-12-31"))
                    .andExpect(status().isOk());

            verify(projectService).listProjects(
                    any(),
                    eq(LocalDate.of(2024, 1, 1)),
                    eq(LocalDate.of(2024, 12, 31)),
                    any(), any(), any(Pageable.class)
            );
        }

        @Test
        @DisplayName("Should filter projects by code and name")
        void shouldFilterProjectsByCodeAndName() throws Exception {
            // Given
            PageImpl<ProjectDTO> projectPage = new PageImpl<>(List.of(sampleProject));
            when(projectService.listProjects(any(), any(), any(), eq("TEST"), eq("Project"), any(Pageable.class)))
                    .thenReturn(projectPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects")
                            .param("code", "TEST")
                            .param("name", "Project"))
                    .andExpect(status().isOk());

            verify(projectService).listProjects(any(), any(), any(), eq("TEST"), eq("Project"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{id} - Get Project by ID")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return project when found")
        void shouldReturnProjectWhenFound() throws Exception {
            // Given
            when(projectService.getProjectById(1L)).thenReturn(sampleProject);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.code").value("TEST-2024"))
                    .andExpect(jsonPath("$.name").value("Test Project"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(projectService).getProjectById(1L);
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectNotFound() throws Exception {
            // Given
            when(projectService.getProjectById(999L))
                    .thenThrow(new ProjectNotFoundException(999L));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.detail").value("Project not found"));

            verify(projectService).getProjectById(999L);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/projects/{id} - Update Project")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project successfully")
        void shouldUpdateProjectSuccessfully() throws Exception {
            // Given
            ProjectDTO updatedProject = ProjectDTO.builder()
                    .id(1L)
                    .code("UPDATED-2024")
                    .name("Updated Project")
                    .description("Updated description")
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .endDate(LocalDate.of(2024, 6, 30))
                    .build();

            when(projectService.updateProject(eq(1L), any(ProjectDTO.class)))
                    .thenReturn(updatedProject);

            // When & Then
            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleProject)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.code").value("UPDATED-2024"))
                    .andExpect(jsonPath("$.name").value("Updated Project"));

            verify(projectService).updateProject(eq(1L), any(ProjectDTO.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid project data")
        void shouldReturn400ForInvalidProjectData() throws Exception {
            // Given
            ProjectDTO invalidProject = ProjectDTO.builder()
                    .code("x") // Too short
                    .name("") // Blank
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .build();

            // When & Then
            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidProject)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors").isArray());

            verify(projectService, never()).updateProject(any(), any());
        }

        @Test
        @DisplayName("Should return 409 for duplicate project code")
        void shouldReturn409ForDuplicateProjectCode() throws Exception {
            // Given
            when(projectService.updateProject(eq(1L), any(ProjectDTO.class)))
                    .thenThrow(new BusinessConflictException("Project with code 'TEST-2024' already exists"));

            // When & Then
            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleProject)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflict"))
                    .andExpect(jsonPath("$.detail").value("Project with code 'TEST-2024' already exists"));

            verify(projectService).updateProject(eq(1L), any(ProjectDTO.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/projects/{id} - Patch Project")
    class PatchProjectTests {

        @Test
        @DisplayName("Should patch project successfully")
        void shouldPatchProjectSuccessfully() throws Exception {
            // Given
            ProjectDTO patchedProject = ProjectDTO.builder()
                    .id(1L)
                    .code("TEST-2024")
                    .name("Updated Project Name")
                    .description("Updated description")
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .endDate(LocalDate.of(2024, 6, 30))
                    .build();

            when(projectService.patchProject(eq(1L), any(ProjectPatchDTO.class)))
                    .thenReturn(patchedProject);

            // When & Then
            mockMvc.perform(patch("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(samplePatchDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Updated Project Name"))
                    .andExpect(jsonPath("$.description").value("Updated description"));

            verify(projectService).patchProject(eq(1L), any(ProjectPatchDTO.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid patch data")
        void shouldReturn400ForInvalidPatchData() throws Exception {
            // Given
            ProjectPatchDTO invalidPatch = ProjectPatchDTO.builder()
                    .code("x") // Too short
                    .build();

            // When & Then
            mockMvc.perform(patch("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidPatch)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"));

            verify(projectService, never()).patchProject(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/projects/{id} - Delete Project")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project successfully")
        void shouldDeleteProjectSuccessfully() throws Exception {
            // Given
            doNothing().when(projectService).deleteProject(1L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1"))
                    .andExpect(status().isNoContent());

            verify(projectService).deleteProject(1L);
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectNotFound() throws Exception {
            // Given
            doThrow(new ProjectNotFoundException(999L))
                    .when(projectService).deleteProject(999L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));

            verify(projectService).deleteProject(999L);
        }

        @Test
        @DisplayName("Should return 409 when project has dependencies")
        void shouldReturn409WhenProjectHasDependencies() throws Exception {
            // Given
            doThrow(new BusinessConflictException("Cannot delete project with active members"))
                    .when(projectService).deleteProject(1L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflict"));

            verify(projectService).deleteProject(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{id}/members - Get Project Members")
    class GetProjectMembersTests {

        @Test
        @DisplayName("Should return project members without enrichment")
        void shouldReturnProjectMembersWithoutEnrichment() throws Exception {
            // Given
            PageImpl<ProjectMemberDTO> membersPage = new PageImpl<>(
                    List.of(sampleMember),
                    PageRequest.of(0, 50),
                    1
            );
            when(projectService.getProjectMembers(eq(1L), eq(false), any(Pageable.class)))
                    .thenReturn(membersPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members")
                            .param("enrich", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].employeeId").value(123))
                    .andExpect(jsonPath("$.content[0].role").value("Developer"))
                    .andExpect(jsonPath("$.content[0].allocationPercent").value(75));

            verify(projectService).getProjectMembers(eq(1L), eq(false), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return project members with enrichment")
        void shouldReturnProjectMembersWithEnrichment() throws Exception {
            // Given
            EmployeeDTO employee = EmployeeDTO.builder()
                    .id(123L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@company.com")
                    .build();

            ProjectMemberDTO enrichedMember = ProjectMemberDTO.builder()
                    .id(1L)
                    .projectId(1L)
                    .employeeId(123L)
                    .role("Developer")
                    .allocationPercent(75)
                    .assignedAt(LocalDateTime.now())
                    .employee(employee)
                    .build();

            PageImpl<ProjectMemberDTO> membersPage = new PageImpl<>(List.of(enrichedMember));
            when(projectService.getProjectMembers(eq(1L), eq(true), any(Pageable.class)))
                    .thenReturn(membersPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members")
                            .param("enrich", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employee").exists())
                    .andExpect(jsonPath("$.content[0].employee.firstName").value("John"))
                    .andExpect(jsonPath("$.content[0].employee.lastName").value("Doe"));

            verify(projectService).getProjectMembers(eq(1L), eq(true), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectNotFound() throws Exception {
            // Given
            when(projectService.getProjectMembers(eq(999L), eq(false), any(Pageable.class)))
                    .thenThrow(new ProjectNotFoundException(999L));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/999/members"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));

            verify(projectService).getProjectMembers(eq(999L), eq(false), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/projects/{projectId}/members - Add Project Members")
    class AddProjectMembersTests {

        @Test
        @DisplayName("Should add project members successfully")
        void shouldAddProjectMembersSuccessfully() throws Exception {
            // Given
            List<ProjectMemberDTO> memberRequests = Arrays.asList(
                    ProjectMemberDTO.builder()
                            .employeeId(123L)
                            .role("Developer")
                            .allocationPercent(75)
                            .build(),
                    ProjectMemberDTO.builder()
                            .employeeId(124L)
                            .role("Tester")
                            .allocationPercent(50)
                            .build()
            );

            List<ProjectMemberDTO> createdMembers = Arrays.asList(
                    ProjectMemberDTO.builder()
                            .id(1L)
                            .projectId(1L)
                            .employeeId(123L)
                            .role("Developer")
                            .allocationPercent(75)
                            .assignedAt(LocalDateTime.now())
                            .build(),
                    ProjectMemberDTO.builder()
                            .id(2L)
                            .projectId(1L)
                            .employeeId(124L)
                            .role("Tester")
                            .allocationPercent(50)
                            .assignedAt(LocalDateTime.now())
                            .build()
            );

            when(projectService.addProjectMembers(eq(1L), any(List.class)))
                    .thenReturn(createdMembers);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].employeeId").value(123))
                    .andExpect(jsonPath("$[0].role").value("Developer"))
                    .andExpect(jsonPath("$[1].employeeId").value(124))
                    .andExpect(jsonPath("$[1].role").value("Tester"));

            verify(projectService).addProjectMembers(eq(1L), any(List.class));
        }

        @Test
        @DisplayName("Should return 400 for empty member requests")
        void shouldReturn400ForEmptyMemberRequests() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));

            verify(projectService, never()).addProjectMembers(any(), any());
        }

        @Test
        @DisplayName("Should return 400 for invalid member data")
        void shouldReturn400ForInvalidMemberData() throws Exception {
            // Given
            List<ProjectMemberDTO> invalidMembers = Arrays.asList(
                    ProjectMemberDTO.builder()
                            .employeeId(null) // Required field missing
                            .role("") // Blank role
                            .allocationPercent(150) // Over 100%
                            .build()
            );

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidMembers)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors").isArray());

            verify(projectService, never()).addProjectMembers(any(), any());
        }

        @Test
        @DisplayName("Should return 409 for duplicate member")
        void shouldReturn409ForDuplicateMember() throws Exception {
            // Given
            List<ProjectMemberDTO> memberRequests = Arrays.asList(
                    ProjectMemberDTO.builder()
                            .employeeId(123L)
                            .role("Developer")
                            .allocationPercent(75)
                            .build()
            );

            when(projectService.addProjectMembers(eq(1L), any(List.class)))
                    .thenThrow(new BusinessConflictException("Employee is already a member of this project"));

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequests)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflict"));

            verify(projectService).addProjectMembers(eq(1L), any(List.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/projects/{projectId}/members/{employeeId} - Remove Project Member")
    class RemoveProjectMemberTests {

        @Test
        @DisplayName("Should remove project member successfully")
        void shouldRemoveProjectMemberSuccessfully() throws Exception {
            // Given
            doNothing().when(projectService).removeProjectMember(1L, 123L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1/members/123"))
                    .andExpect(status().isNoContent());

            verify(projectService).removeProjectMember(1L, 123L);
        }

        @Test
        @DisplayName("Should return 404 when member not found")
        void shouldReturn404WhenMemberNotFound() throws Exception {
            // Given
            doThrow(new ProjectMemberNotFoundException("Employee 999 is not a member of project 1"))
                    .when(projectService).removeProjectMember(1L, 999L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1/members/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Project Member Not Found"));

            verify(projectService).removeProjectMember(1L, 999L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/stats - Get Project Statistics")
    class GetProjectStatsTests {

        @Test
        @DisplayName("Should return project stats grouped by status")
        void shouldReturnProjectStatsGroupedByStatus() throws Exception {
            // Given
            ProjectStatsDTO statsDTO = ProjectStatsDTO.builder()
                    .groupBy("status")
                    .stats(Arrays.asList(
                            ProjectStatItem.builder()
                                    .label("ACTIVE")
                                    .count(15L)
                                    .displayName("Active Projects")
                                    .build(),
                            ProjectStatItem.builder()
                                    .label("COMPLETED")
                                    .count(8L)
                                    .displayName("Completed Projects")
                                    .build()
                    ))
                    .build();

            when(projectService.getProjectStats("status")).thenReturn(statsDTO);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/stats")
                            .param("groupBy", "status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupBy").value("status"))
                    .andExpect(jsonPath("$.stats").isArray())
                    .andExpect(jsonPath("$.stats", hasSize(2)))
                    .andExpect(jsonPath("$.stats[0].label").value("ACTIVE"))
                    .andExpect(jsonPath("$.stats[0].count").value(15))
                    .andExpect(jsonPath("$.stats[0].displayName").value("Active Projects"));

            verify(projectService).getProjectStats("status");
        }

        @Test
        @DisplayName("Should return project stats grouped by month")
        void shouldReturnProjectStatsGroupedByMonth() throws Exception {
            // Given
            ProjectStatsDTO statsDTO = ProjectStatsDTO.builder()
                    .groupBy("month")
                    .stats(Arrays.asList(
                            ProjectStatItem.builder()
                                    .label("2024-01")
                                    .count(5L)
                                    .displayName("January 2024")
                                    .build()
                    ))
                    .build();

            when(projectService.getProjectStats("month")).thenReturn(statsDTO);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/stats")
                            .param("groupBy", "month"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupBy").value("month"))
                    .andExpect(jsonPath("$.stats[0].label").value("2024-01"))
                    .andExpect(jsonPath("$.stats[0].displayName").value("January 2024"));

            verify(projectService).getProjectStats("month");
        }

        @Test
        @DisplayName("Should use default groupBy when not provided")
        void shouldUseDefaultGroupByWhenNotProvided() throws Exception {
            // Given
            ProjectStatsDTO statsDTO = ProjectStatsDTO.builder()
                    .groupBy("status")
                    .stats(Arrays.asList())
                    .build();

            when(projectService.getProjectStats("status")).thenReturn(statsDTO);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupBy").value("status"));

            verify(projectService).getProjectStats("status");
        }

        @Test
        @DisplayName("Should return 400 for invalid groupBy parameter")
        void shouldReturn400ForInvalidGroupByParameter() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/projects/stats")
                            .param("groupBy", "invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));

            verify(projectService, never()).getProjectStats(any());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            String invalidJson = "{\"name\": \"test\", \"invalid\": }";
            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("Should handle missing request body")
        void shouldHandleMissingRequestBody() throws Exception {
            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle invalid path parameter types")
        void shouldHandleInvalidPathParameterTypes() throws Exception {
            mockMvc.perform(get("/api/v1/projects/invalid-id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Parameter Type"));
        }
    }
}