package com.example.project;

import com.example.project.client.EmployeeServiceClient;
import com.example.project.domain.Project;
import com.example.project.domain.ProjectMember;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import com.example.project.repo.ProjectStatProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
        }
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("Project Service Integration Tests (No DB)")
class ProjectServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private ProjectMemberRepository projectMemberRepository;

    @MockBean
    private EmployeeServiceClient employeeServiceClient;

    private String baseUrl;
    private Project sampleProject1;
    private Project sampleProject2;
    private Project sampleProject3;
    private ProjectDTO sampleProjectDTO;
    private ProjectMember sampleMember1;
    private ProjectMember sampleMember2;
    private EmployeeDTO sampleEmployee1;
    private EmployeeDTO sampleEmployee2;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/projects";
        Mockito.reset(projectRepository, projectMemberRepository, employeeServiceClient);

        // Sample employees
        sampleEmployee1 = EmployeeDTO.builder()
                .id(101L)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@company.com")
                .build();

        sampleEmployee2 = EmployeeDTO.builder()
                .id(102L)
                .firstName("Bob")
                .lastName("Smith")
                .email("bob.smith@company.com")
                .build();

        // Sample projects
        sampleProject1 = Project.builder()
                .id(1L)
                .code("WEB-2024")
                .name("Website Redesign")
                .description("Complete redesign of company website")
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 15))
                .endDate(LocalDate.of(2024, 6, 30))
                .build();

        sampleProject2 = Project.builder()
                .id(2L)
                .code("MOB-2024")
                .name("Mobile App")
                .description("New mobile application")
                .status(ProjectStatus.PLANNED)
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        sampleProject3 = Project.builder()
                .id(3L)
                .code("API-2024")
                .name("API Modernization")
                .description("Modernize legacy APIs")
                .status(ProjectStatus.COMPLETED)
                .startDate(LocalDate.of(2023, 6, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .build();

        // Sample project DTO for creation/updates
        sampleProjectDTO = ProjectDTO.builder()
                .code("TEST-2024")
                .name("Test Project")
                .description("A test project for integration tests")
                .status(ProjectStatus.PLANNED)
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 8, 31))
                .build();

        // Sample project members
        sampleMember1 = ProjectMember.builder()
                .id(1L)
                .project(sampleProject1)
                .employeeId(101L)
                .role("Frontend Developer")
                .allocationPercent(75)
                .assignedAt(LocalDateTime.now().minusDays(10))
                .build();

        sampleMember2 = ProjectMember.builder()
                .id(2L)
                .project(sampleProject1)
                .employeeId(102L)
                .role("Backend Developer")
                .allocationPercent(80)
                .assignedAt(LocalDateTime.now().minusDays(8))
                .build();
    }

    // ========================================
    // LIST PROJECTS TESTS
    // ========================================

    @Test
    @DisplayName("GET /projects - should return paginated projects")
    void listProjects_ShouldReturnPaginatedResults() {
        // Arrange
        List<Project> projects = List.of(sampleProject1, sampleProject2, sampleProject3);
        Page<Project> projectPage = new PageImpl<>(projects, PageRequest.of(0, 20), 3);

        when(projectRepository.findProjectsWithFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(projectPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(3);
        assertThat(response.getBody().get("totalElements")).isEqualTo(3);
        assertThat(response.getBody().get("totalPages")).isEqualTo(1);

        Map<String, Object> firstProject = content.get(0);
        assertThat(firstProject.get("id")).isEqualTo(1);
        assertThat(firstProject.get("code")).isEqualTo("WEB-2024");
        assertThat(firstProject.get("name")).isEqualTo("Website Redesign");
        assertThat(firstProject.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("GET /projects with status filter - should filter by status")
    void listProjects_WithStatusFilter_ShouldReturnFilteredResults() {
        // Arrange
        List<Project> filteredProjects = List.of(sampleProject1);
        Page<Project> projectPage = new PageImpl<>(filteredProjects, PageRequest.of(0, 20), 1);

        when(projectRepository.findProjectsWithFilters(
                eq(ProjectStatus.ACTIVE), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(projectPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?status=ACTIVE&page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("GET /projects with date range filter - should filter by date range")
    void listProjects_WithDateRangeFilter_ShouldReturnFilteredResults() {
        // Arrange
        List<Project> filteredProjects = List.of(sampleProject1, sampleProject2);
        Page<Project> projectPage = new PageImpl<>(filteredProjects, PageRequest.of(0, 20), 2);

        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        when(projectRepository.findProjectsWithFilters(
                isNull(), eq(from), eq(to), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(projectPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?from=2024-01-01&to=2024-12-31&page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2);
    }

    @Test
    @DisplayName("GET /projects with code filter - should filter by code")
    void listProjects_WithCodeFilter_ShouldReturnFilteredResults() {
        // Arrange
        List<Project> filteredProjects = List.of(sampleProject1);
        Page<Project> projectPage = new PageImpl<>(filteredProjects, PageRequest.of(0, 20), 1);

        when(projectRepository.findProjectsWithFilters(
                isNull(), isNull(), isNull(), eq("WEB-2024"), isNull(), any(Pageable.class)))
                .thenReturn(projectPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?code=WEB-2024&page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("code")).isEqualTo("WEB-2024");
    }

    // ========================================
    // GET PROJECT BY ID TESTS
    // ========================================

    @Test
    @DisplayName("GET /projects/{id} - should return project when found")
    void getProjectById_WhenExists_ShouldReturnProject() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject1));

        // Act
        ResponseEntity<ProjectDTO> response = restTemplate.getForEntity(
                baseUrl + "/1",
                ProjectDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getCode()).isEqualTo("WEB-2024");
        assertThat(response.getBody().getName()).isEqualTo("Website Redesign");
        assertThat(response.getBody().getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("GET /projects/{id} - should return 404 when not found")
    void getProjectById_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title")).isEqualTo("Resource Not Found");
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    // ========================================
    // UPDATE PROJECT TESTS
    // ========================================

    @Test
    @DisplayName("PUT /projects/{id} - should update project successfully")
    void updateProject_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        Project updatedProject = Project.builder()
                .id(1L)
                .code("WEB-2024-UPDATED")
                .name("Website Redesign Updated")
                .description("Updated description")
                .status(ProjectStatus.ON_HOLD)
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 7, 31))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject1));
        when(projectRepository.existsByCodeAndIdNot("WEB-2024-UPDATED", 1L)).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(updatedProject);

        ProjectDTO updateDto = ProjectDTO.builder()
                .code("WEB-2024-UPDATED")
                .name("Website Redesign Updated")
                .description("Updated description")
                .status(ProjectStatus.ON_HOLD)
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 7, 31))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectDTO> request = new HttpEntity<>(updateDto, headers);

        // Act
        ResponseEntity<ProjectDTO> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PUT,
                request,
                ProjectDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getCode()).isEqualTo("WEB-2024-UPDATED");
        assertThat(response.getBody().getName()).isEqualTo("Website Redesign Updated");
        assertThat(response.getBody().getStatus()).isEqualTo(ProjectStatus.ON_HOLD);
    }

    @Test
    @DisplayName("PUT /projects/{id} - should return 404 when project not found")
    void updateProject_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectDTO> request = new HttpEntity<>(sampleProjectDTO, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /projects/{id} - should return 409 when code conflicts")
    void updateProject_WithConflictingCode_ShouldReturn409() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject1));
        when(projectRepository.existsByCodeAndIdNot("MOB-2024", 1L)).thenReturn(true);

        ProjectDTO updateDto = ProjectDTO.builder()
                .code("MOB-2024") // Conflicting code
                .name("Updated Project")
                .description("Updated description")
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectDTO> request = new HttpEntity<>(updateDto, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Conflict");
    }

    // ========================================
    // PATCH PROJECT TESTS
    // ========================================

    @Test
    @DisplayName("PATCH /projects/{id} - should partially update project")
    void patchProject_WithPartialData_ShouldUpdateSuccessfully() {
        // Arrange
        Project patchedProject = Project.builder()
                .id(1L)
                .code("WEB-2024")
                .name("Website Redesign")
                .description("Updated description only")
                .status(ProjectStatus.ON_HOLD) // Only status and description changed
                .startDate(LocalDate.of(2024, 1, 15))
                .endDate(LocalDate.of(2024, 6, 30))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject1));
        when(projectRepository.save(any(Project.class))).thenReturn(patchedProject);

        ProjectPatchDTO patchDto = ProjectPatchDTO.builder()
                .description("Updated description only")
                .status(ProjectStatus.ON_HOLD)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectPatchDTO> request = new HttpEntity<>(patchDto, headers);

        // Act
        ResponseEntity<ProjectDTO> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PATCH,
                request,
                ProjectDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getCode()).isEqualTo("WEB-2024"); // Unchanged
        assertThat(response.getBody().getName()).isEqualTo("Website Redesign"); // Unchanged
        assertThat(response.getBody().getDescription()).isEqualTo("Updated description only"); // Changed
        assertThat(response.getBody().getStatus()).isEqualTo(ProjectStatus.ON_HOLD); // Changed
    }

    // ========================================
    // DELETE PROJECT TESTS
    // ========================================

    @Test
    @DisplayName("DELETE /projects/{id} - should delete project successfully when no members")
    void deleteProject_WithNoMembers_ShouldDeleteSuccessfully() {
        // Arrange
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.countByProject_Id(1L)).thenReturn(0L);

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("DELETE /projects/{id} - should return 409 when project has members")
    void deleteProject_WithMembers_ShouldReturn409() {
        // Arrange
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.countByProject_Id(1L)).thenReturn(2L);
        when(projectMemberRepository.findEmployeeIdsByProjectId(1L)).thenReturn(List.of(101L, 102L));
        when(projectRepository.findCodeById(1L)).thenReturn(Optional.of("WEB-2024"));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Conflict");
        assertThat(response.getBody().get("detail")).isEqualTo("Cannot delete project with active members");

        @SuppressWarnings("unchecked")
        Map<String, Object> conflictDetails = (Map<String, Object>) response.getBody().get("conflictDetails");
        assertThat(conflictDetails.get("memberCount")).isEqualTo(2);
        assertThat(conflictDetails.get("projectCode")).isEqualTo("WEB-2024");
    }

    @Test
    @DisplayName("DELETE /projects/{id} - should return 404 when project not found")
    void deleteProject_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(projectRepository.existsById(999L)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========================================
    // PROJECT MEMBERS TESTS
    // ========================================

    @Test
    @DisplayName("GET /projects/{id}/members - should return project members without enrichment")
    void getProjectMembers_WithoutEnrichment_ShouldReturnBasicMembers() {
        // Arrange
        List<ProjectMember> members = List.of(sampleMember1, sampleMember2);
        Page<ProjectMember> memberPage = new PageImpl<>(members, PageRequest.of(0, 50), 2);

        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.findByProject_Id(eq(1L), any(Pageable.class))).thenReturn(memberPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members?enrich=false&page=0&size=50",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);

        Map<String, Object> firstMember = content.get(0);
        assertThat(firstMember.get("employeeId")).isEqualTo(101);
        assertThat(firstMember.get("role")).isEqualTo("Frontend Developer");
        assertThat(firstMember.get("allocationPercent")).isEqualTo(75);
        assertThat(firstMember.get("employee")).isNull(); // No enrichment
    }

    @Test
    @DisplayName("GET /projects/{id}/members - should return project members with enrichment")
    void getProjectMembers_WithEnrichment_ShouldReturnEnrichedMembers() {
        // Arrange
        List<ProjectMember> members = List.of(sampleMember1, sampleMember2);
        Page<ProjectMember> memberPage = new PageImpl<>(members, PageRequest.of(0, 50), 2);

        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.findByProject_Id(eq(1L), any(Pageable.class))).thenReturn(memberPage);
        when(employeeServiceClient.getEmployeeBasic(101L)).thenReturn(sampleEmployee1);
        when(employeeServiceClient.getEmployeeBasic(102L)).thenReturn(sampleEmployee2);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members?enrich=true&page=0&size=50",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2);

        Map<String, Object> firstMember = content.get(0);
        assertThat(firstMember.get("employeeId")).isEqualTo(101);
        assertThat(firstMember.get("role")).isEqualTo("Frontend Developer");
        assertThat(firstMember.get("allocationPercent")).isEqualTo(75);

        @SuppressWarnings("unchecked")
        Map<String, Object> employee = (Map<String, Object>) firstMember.get("employee");
        assertThat(employee).isNotNull();
        assertThat(employee.get("id")).isEqualTo(101);
        assertThat(employee.get("firstName")).isEqualTo("Alice");
        assertThat(employee.get("lastName")).isEqualTo("Johnson");
    }

    @Test
    @DisplayName("GET /projects/{id}/members - should return 404 when project not found")
    void getProjectMembers_WhenProjectNotExists_ShouldReturn404() {
        // Arrange
        when(projectRepository.existsById(999L)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999/members",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========================================
    // ADD PROJECT MEMBERS TESTS
    // ========================================

    @Test
    @DisplayName("POST /projects/{id}/members - should add members successfully")
    void addProjectMembers_WithValidData_ShouldAddSuccessfully() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject1));
        when(employeeServiceClient.getEmployeeBasic(101L)).thenReturn(sampleEmployee1);
        when(employeeServiceClient.getEmployeeBasic(102L)).thenReturn(sampleEmployee2);
        when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 101L)).thenReturn(false);
        when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 102L)).thenReturn(false);
        when(projectMemberRepository.saveAll(any())).thenReturn(List.of(sampleMember1, sampleMember2));

        List<ProjectMemberDTO> memberRequests = List.of(
                ProjectMemberDTO.builder()
                        .employeeId(101L)
                        .role("Frontend Developer")
                        .allocationPercent(75)
                        .build(),
                ProjectMemberDTO.builder()
                        .employeeId(102L)
                        .role("Backend Developer")
                        .allocationPercent(80)
                        .build()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<ProjectMemberDTO>> request = new HttpEntity<>(memberRequests, headers);

        // Act
        ResponseEntity<List<ProjectMemberDTO>> response = restTemplate.exchange(
                baseUrl + "/1/members",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<List<ProjectMemberDTO>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getEmployeeId()).isEqualTo(101L);
        assertThat(response.getBody().get(0).getRole()).isEqualTo("Frontend Developer");
        assertThat(response.getBody().get(1).getEmployeeId()).isEqualTo(102L);
        assertThat(response.getBody().get(1).getRole()).isEqualTo("Backend Developer");
    }

    @Test
    @DisplayName("POST /projects/{id}/members - should return 400 with empty request")
    void addProjectMembers_WithEmptyRequest_ShouldReturn400() {
        // Arrange
        List<ProjectMemberDTO> emptyRequest = List.of();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<ProjectMemberDTO>> request = new HttpEntity<>(emptyRequest, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("title")).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("POST /projects/{id}/members - should return 409 when employee already member")
    void addProjectMembers_WithExistingMember_ShouldReturn409() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject1));
        when(employeeServiceClient.getEmployeeBasic(101L)).thenReturn(sampleEmployee1);
        when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 101L)).thenReturn(true);

        List<ProjectMemberDTO> memberRequests = List.of(
                ProjectMemberDTO.builder()
                        .employeeId(101L)
                        .role("Frontend Developer")
                        .allocationPercent(75)
                        .build()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<ProjectMemberDTO>> request = new HttpEntity<>(memberRequests, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Conflict");
    }

    // ========================================
    // REMOVE PROJECT MEMBER TESTS
    // ========================================

    @Test
    @DisplayName("DELETE /projects/{id}/members/{employeeId} - should remove member successfully")
    void removeProjectMember_WhenMemberExists_ShouldRemoveSuccessfully() {
        // Arrange
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 101L)).thenReturn(true);

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/1/members/101",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("DELETE /projects/{id}/members/{employeeId} - should return 404 when member not found")
    void removeProjectMember_WhenMemberNotExists_ShouldReturn404() {
        // Arrange
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 999L)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members/999",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title")).isEqualTo("Project Member Not Found");
    }

    // ========================================
    // PROJECT STATISTICS TESTS
    // ========================================

    @Test
    @DisplayName("GET /projects/stats?groupBy=status - should return status statistics")
    void getProjectStats_ByStatus_ShouldReturnStatusStats() {
        // Arrange
        List<ProjectStatProjection> statusStats = List.of(
                createProjectStatProjection("ACTIVE", 5L),
                createProjectStatProjection("COMPLETED", 3L),
                createProjectStatProjection("PLANNED", 2L)
        );

        when(projectRepository.countByStatus()).thenReturn(statusStats);

        // Act
        ResponseEntity<ProjectStatsDTO> response = restTemplate.getForEntity(
                baseUrl + "/stats?groupBy=status",
                ProjectStatsDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getGroupBy()).isEqualTo("status");
        assertThat(response.getBody().getStats()).hasSize(3);
        assertThat(response.getBody().getStats().get(0).getLabel()).isEqualTo("ACTIVE");
        assertThat(response.getBody().getStats().get(0).getCount()).isEqualTo(5L);
        assertThat(response.getBody().getStats().get(0).getDisplayName()).isEqualTo("Active");
    }

    @Test
    @DisplayName("GET /projects/stats?groupBy=month - should return month statistics")
    void getProjectStats_ByMonth_ShouldReturnMonthStats() {
        // Arrange
        List<ProjectStatProjection> monthStats = List.of(
                createProjectStatProjection("2024-01", 3L),
                createProjectStatProjection("2024-02", 2L),
                createProjectStatProjection("2024-03", 1L)
        );

        when(projectRepository.countByStartMonth()).thenReturn(monthStats);

        // Act
        ResponseEntity<ProjectStatsDTO> response = restTemplate.getForEntity(
                baseUrl + "/stats?groupBy=month",
                ProjectStatsDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getGroupBy()).isEqualTo("month");
        assertThat(response.getBody().getStats()).hasSize(3);
        assertThat(response.getBody().getStats().get(0).getLabel()).isEqualTo("2024-01");
        assertThat(response.getBody().getStats().get(0).getCount()).isEqualTo(3L);
        assertThat(response.getBody().getStats().get(0).getDisplayName()).isEqualTo("January 2024");
    }

    @Test
    @DisplayName("GET /projects/stats - should return 400 with invalid groupBy")
    void getProjectStats_WithInvalidGroupBy_ShouldReturn400() {
        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/stats?groupBy=invalid",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("title")).isEqualTo("Bad Request");
    }

    // ========================================
    // CREATE PROJECT TESTS
    // ========================================

    @Test
    @DisplayName("POST /projects - should create project successfully")
    void createProject_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        Project savedProject = Project.builder()
                .id(4L)
                .code("TEST-2024")
                .name("Test Project")
                .description("A test project for integration tests")
                .status(ProjectStatus.PLANNED)
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 8, 31))
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectDTO> request = new HttpEntity<>(sampleProjectDTO, headers);

        // Act
        ResponseEntity<ProjectDTO> response = restTemplate.postForEntity(baseUrl, request, ProjectDTO.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(4L);
        assertThat(response.getBody().getCode()).isEqualTo("TEST-2024");
        assertThat(response.getBody().getName()).isEqualTo("Test Project");
        assertThat(response.getBody().getStatus()).isEqualTo(ProjectStatus.PLANNED);
    }

    // ========================================
    // VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("PUT /projects/{id} - should return 400 for invalid project data")
    void updateProject_WithInvalidData_ShouldReturn400() {
        // Arrange
        ProjectDTO invalidDto = ProjectDTO.builder()
                .code("") // Empty code
                .name("AB") // Too short name
                .description(null)
                .status(null) // Null status
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 1, 1)) // End before start
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectDTO> request = new HttpEntity<>(invalidDto, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("title")).isEqualTo("Validation Error");
        assertThat((List<?>) response.getBody().get("errors")).isNotEmpty();
    }

    @Test
    @DisplayName("POST /projects/{id}/members - should return 400 for invalid member data")
    void addProjectMembers_WithInvalidData_ShouldReturn400() {
        // Arrange
        List<ProjectMemberDTO> invalidMembers = List.of(
                ProjectMemberDTO.builder()
                        .employeeId(null) // Null employee ID
                        .role("") // Empty role
                        .allocationPercent(150) // Invalid allocation > 100
                        .build()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<ProjectMemberDTO>> request = new HttpEntity<>(invalidMembers, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("title")).isEqualTo("Validation Error");
    }

    // ========================================
    // EXTERNAL SERVICE FAILURE TESTS
    // ========================================

    @Test
    @DisplayName("Should handle Employee Service failures gracefully")
    void whenEmployeeServiceFails_ShouldHandleGracefully() {
        // Arrange
        List<ProjectMember> members = List.of(sampleMember1);
        Page<ProjectMember> memberPage = new PageImpl<>(members, PageRequest.of(0, 50), 1);

        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.findByProject_Id(eq(1L), any(Pageable.class))).thenReturn(memberPage);
        when(employeeServiceClient.getEmployeeBasic(101L))
                .thenThrow(new RuntimeException("Employee service unavailable"));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1/members?enrich=true",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert - Should return 503 Service Unavailable due to external service exception
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().get("title")).isEqualTo("Service Unavailable");
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private ProjectStatProjection createProjectStatProjection(String label, Long count) {
        return new ProjectStatProjection() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}