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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private EmployeeValidationService employeeValidationService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project testProject;
    private ProjectDTO testProjectDTO;
    private ProjectMember testProjectMember;
    private ProjectMemberDTO testProjectMemberDTO;
    private EmployeeDTO testEmployeeDTO;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(1L)
                .code("PROJ-001")
                .name("Test Project")
                .description("Test project description")
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        testProjectDTO = ProjectDTO.builder()
                .id(1L)
                .code("PROJ-001")
                .name("Test Project")
                .description("Test project description")
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        testProjectMember = ProjectMember.builder()
                .id(1L)
                .project(testProject)
                .employeeId(100L)
                .role("DEVELOPER")
                .allocationPercent(80)
                .build();

        testProjectMemberDTO = ProjectMemberDTO.builder()
                .id(1L)
                .projectId(1L)
                .employeeId(100L)
                .role("DEVELOPER")
                .allocationPercent(80)
                .build();

        testEmployeeDTO = EmployeeDTO.builder()
                .id(100L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();
    }

    @Nested
    @DisplayName("listProjects() Tests")
    class ListProjectsTests {

        @Test
        @DisplayName("Should return paginated projects with filters")
        void shouldReturnPaginatedProjectsWithFilters() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Project> projectPage = new PageImpl<>(List.of(testProject));
            when(projectRepository.findProjectsWithFilters(
                    eq(ProjectStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class),
                    eq("PROJ-001"), eq("Test"), eq(pageable)
            )).thenReturn(projectPage);
            when(projectMapper.toDTO(testProject)).thenReturn(testProjectDTO);

            // When
            Page<ProjectDTO> result = projectService.listProjects(
                    ProjectStatus.ACTIVE, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                    "PROJ-001", "Test", pageable
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCode()).isEqualTo("PROJ-001");
            verify(projectRepository).findProjectsWithFilters(
                    eq(ProjectStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class),
                    eq("PROJ-001"), eq("Test"), eq(pageable)
            );
            verify(projectMapper).toDTO(testProject);
        }

        @Test
        @DisplayName("Should return empty page when no projects found")
        void shouldReturnEmptyPageWhenNoProjectsFound() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Project> emptyPage = new PageImpl<>(Collections.emptyList());
            when(projectRepository.findProjectsWithFilters(any(), any(), any(), any(), any(), any()))
                    .thenReturn(emptyPage);

            // When
            Page<ProjectDTO> result = projectService.listProjects(
                    null, null, null, null, null, pageable
            );

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getProjectById() Tests")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return project by ID successfully")
        void shouldReturnProjectByIdSuccessfully() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectMapper.toDTO(testProject)).thenReturn(testProjectDTO);

            // When
            ProjectDTO result = projectService.getProjectById(1L);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("PROJ-001");
            verify(projectRepository).findById(1L);
            verify(projectMapper).toDTO(testProject);
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when project not found")
        void shouldThrowProjectNotFoundExceptionWhenProjectNotFound() {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.getProjectById(999L))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("createProject() Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project successfully")
        void shouldCreateProjectSuccessfully() {
            // Given
            ProjectDTO newProjectDTO = ProjectDTO.builder()
                    .code("PROJ-002")
                    .name("New Project")
                    .description("New project description")
                    .status(ProjectStatus.PLANNED)
                    .startDate(LocalDate.of(2024, 3, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .build();

            Project newProject = Project.builder()
                    .code("PROJ-002")
                    .name("New Project")
                    .description("New project description")
                    .status(ProjectStatus.PLANNED)
                    .startDate(LocalDate.of(2024, 3, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .build();

            Project savedProject = Project.builder()
                    .id(2L)
                    .code("PROJ-002")
                    .name("New Project")
                    .description("New project description")
                    .status(ProjectStatus.PLANNED)
                    .startDate(LocalDate.of(2024, 3, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .build();

            ProjectDTO savedProjectDTO = ProjectDTO.builder()
                    .id(2L)
                    .code("PROJ-002")
                    .name("New Project")
                    .description("New project description")
                    .status(ProjectStatus.PLANNED)
                    .startDate(LocalDate.of(2024, 3, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .build();

            when(projectMapper.toEntity(newProjectDTO)).thenReturn(newProject);
            when(projectRepository.save(newProject)).thenReturn(savedProject);
            when(projectMapper.toDTO(savedProject)).thenReturn(savedProjectDTO);

            // When
            ProjectDTO result = projectService.createProject(newProjectDTO);

            // Then
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getCode()).isEqualTo("PROJ-002");
            verify(projectMapper).toEntity(newProjectDTO);
            verify(projectRepository).save(newProject);
            verify(projectMapper).toDTO(savedProject);
        }
    }

    @Nested
    @DisplayName("updateProject() Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project successfully")
        void shouldUpdateProjectSuccessfully() {
            // Given
            ProjectDTO updateDTO = ProjectDTO.builder()
                    .code("PROJ-001-UPDATED")
                    .name("Updated Project")
                    .description("Updated description")
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2024, 2, 1))
                    .endDate(LocalDate.of(2024, 11, 30))
                    .build();

            Project updatedProject = Project.builder()
                    .id(1L)
                    .code("PROJ-001-UPDATED")
                    .name("Updated Project")
                    .description("Updated description")
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2024, 2, 1))
                    .endDate(LocalDate.of(2024, 11, 30))
                    .build();

            ProjectDTO updatedProjectDTO = ProjectDTO.builder()
                    .id(1L)
                    .code("PROJ-001-UPDATED")
                    .name("Updated Project")
                    .description("Updated description")
                    .status(ProjectStatus.COMPLETED)
                    .startDate(LocalDate.of(2024, 2, 1))
                    .endDate(LocalDate.of(2024, 11, 30))
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByCodeAndIdNot("PROJ-001-UPDATED", 1L)).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(updatedProject);
            when(projectMapper.toDTO(updatedProject)).thenReturn(updatedProjectDTO);

            // When
            ProjectDTO result = projectService.updateProject(1L, updateDTO);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("PROJ-001-UPDATED");
            assertThat(result.getName()).isEqualTo("Updated Project");
            verify(projectRepository).findById(1L);
            verify(projectRepository).existsByCodeAndIdNot("PROJ-001-UPDATED", 1L);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when updating non-existent project")
        void shouldThrowProjectNotFoundExceptionWhenUpdatingNonExistentProject() {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.updateProject(999L, testProjectDTO))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).findById(999L);
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw BusinessConflictException when code already exists")
        void shouldThrowBusinessConflictExceptionWhenCodeAlreadyExists() {
            // Given
            ProjectDTO updateDTO = ProjectDTO.builder()
                    .code("EXISTING-CODE")
                    .name("Updated Project")
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByCodeAndIdNot("EXISTING-CODE", 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> projectService.updateProject(1L, updateDTO))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("Project code already exists");
            verify(projectRepository).findById(1L);
            verify(projectRepository).existsByCodeAndIdNot("EXISTING-CODE", 1L);
            verify(projectRepository, never()).save(any(Project.class));
        }
    }

    @Nested
    @DisplayName("patchProject() Tests")
    class PatchProjectTests {

        @Test
        @DisplayName("Should patch project with partial updates")
        void shouldPatchProjectWithPartialUpdates() {
            // Given
            ProjectPatchDTO patchDTO = new ProjectPatchDTO();
            patchDTO.setName("Patched Name");
            patchDTO.setStatus(ProjectStatus.ON_HOLD);

            Project patchedProject = Project.builder()
                    .id(1L)
                    .code("PROJ-001") // unchanged
                    .name("Patched Name") // updated
                    .description("Test project description") // unchanged
                    .status(ProjectStatus.ON_HOLD) // updated
                    .startDate(LocalDate.of(2024, 1, 1)) // unchanged
                    .endDate(LocalDate.of(2024, 12, 31)) // unchanged
                    .build();

            ProjectDTO patchedProjectDTO = ProjectDTO.builder()
                    .id(1L)
                    .code("PROJ-001")
                    .name("Patched Name")
                    .description("Test project description")
                    .status(ProjectStatus.ON_HOLD)
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(Project.class))).thenReturn(patchedProject);
            when(projectMapper.toDTO(patchedProject)).thenReturn(patchedProjectDTO);

            // When
            ProjectDTO result = projectService.patchProject(1L, patchDTO);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Patched Name");
            assertThat(result.getStatus()).isEqualTo(ProjectStatus.ON_HOLD);
            assertThat(result.getCode()).isEqualTo("PROJ-001"); // unchanged
            verify(projectRepository).findById(1L);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when patching non-existent project")
        void shouldThrowProjectNotFoundExceptionWhenPatchingNonExistentProject() {
            // Given
            ProjectPatchDTO patchDTO = new ProjectPatchDTO();
            patchDTO.setName("Patched Name");

            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.patchProject(999L, patchDTO))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw BusinessConflictException when patching to duplicate code")
        void shouldThrowBusinessConflictExceptionWhenPatchingToDuplicateCode() {
            // Given
            ProjectPatchDTO patchDTO = new ProjectPatchDTO();
            patchDTO.setCode("EXISTING-CODE");

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByCodeAndIdNot("EXISTING-CODE", 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> projectService.patchProject(1L, patchDTO))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("Project code already exists");
            verify(projectRepository).findById(1L);
            verify(projectRepository).existsByCodeAndIdNot("EXISTING-CODE", 1L);
        }
    }

    @Nested
    @DisplayName("getProjectMembers() Tests")
    class GetProjectMembersTests {

        @Test
        @DisplayName("Should return basic project members without enrichment")
        void shouldReturnBasicProjectMembersWithoutEnrichment() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectMember> memberPage = new PageImpl<>(List.of(testProjectMember));

            when(projectRepository.existsById(1L)).thenReturn(true);
            when(projectMemberRepository.findByProject_Id(1L, pageable)).thenReturn(memberPage);
            when(projectMapper.memberToDTO(testProjectMember)).thenReturn(testProjectMemberDTO);

            // When
            Page<ProjectMemberDTO> result = projectService.getProjectMembers(1L, false, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmployeeId()).isEqualTo(100L);
            assertThat(result.getContent().get(0).getEmployee()).isNull();
            verify(projectRepository).existsById(1L);
            verify(projectMemberRepository).findByProject_Id(1L, pageable);
            verify(employeeValidationService, never()).getEmployeesBasic(any());
        }

        @Test
        @DisplayName("Should return enriched project members with employee details")
        void shouldReturnEnrichedProjectMembersWithEmployeeDetails() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectMember> memberPage = new PageImpl<>(List.of(testProjectMember));
            Set<Long> employeeIds = Set.of(100L);
            List<EmployeeDTO> employees = List.of(testEmployeeDTO);

            ProjectMemberDTO enrichedMemberDTO = ProjectMemberDTO.builder()
                    .id(1L)
                    .projectId(1L)
                    .employeeId(100L)
                    .role("DEVELOPER")
                    .allocationPercent(80)
                    .employee(testEmployeeDTO)
                    .build();

            when(projectRepository.existsById(1L)).thenReturn(true);
            when(projectMemberRepository.findByProject_Id(1L, pageable)).thenReturn(memberPage);
            when(projectMapper.memberToDTO(testProjectMember)).thenReturn(testProjectMemberDTO);
            when(employeeValidationService.getEmployeesBasic(employeeIds)).thenReturn(employees);

            // When
            Page<ProjectMemberDTO> result = projectService.getProjectMembers(1L, true, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmployeeId()).isEqualTo(100L);
            assertThat(result.getContent().get(0).getEmployee()).isNotNull();
            assertThat(result.getContent().get(0).getEmployee().getFirstName()).isEqualTo("John");
            verify(employeeValidationService).getEmployeesBasic(employeeIds);
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when project does not exist")
        void shouldThrowProjectNotFoundExceptionWhenProjectDoesNotExist() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(projectRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> projectService.getProjectMembers(999L, false, pageable))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).existsById(999L);
        }
    }

    @Nested
    @DisplayName("deleteProject() Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project successfully when no members exist")
        void shouldDeleteProjectSuccessfullyWhenNoMembersExist() {
            // Given
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(projectMemberRepository.countByProject_Id(1L)).thenReturn(0L);

            // When
            projectService.deleteProject(1L);

            // Then
            verify(projectRepository).existsById(1L);
            verify(projectMemberRepository).countByProject_Id(1L);
            verify(projectRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when project does not exist")
        void shouldThrowProjectNotFoundExceptionWhenProjectDoesNotExist() {
            // Given
            when(projectRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> projectService.deleteProject(999L))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).existsById(999L);
            verify(projectRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should throw BusinessConflictException when project has members")
        void shouldThrowBusinessConflictExceptionWhenProjectHasMembers() {
            // Given
            List<Long> memberIds = List.of(100L, 200L);
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(projectMemberRepository.countByProject_Id(1L)).thenReturn(2L);
            when(projectMemberRepository.findEmployeeIdsByProjectId(1L)).thenReturn(memberIds);
            when(projectRepository.findCodeById(1L)).thenReturn(Optional.of("PROJ-001"));

            // When & Then
            assertThatThrownBy(() -> projectService.deleteProject(1L))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("Cannot delete project with active members");
            verify(projectRepository).existsById(1L);
            verify(projectMemberRepository).countByProject_Id(1L);
            verify(projectRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("addProjectMembers() Tests")
    class AddProjectMembersTests {

        @Test
        @DisplayName("Should add project members successfully")
        void shouldAddProjectMembersSuccessfully() {
            // Given
            List<ProjectMemberDTO> memberRequests = List.of(
                    ProjectMemberDTO.builder()
                            .employeeId(100L)
                            .role("DEVELOPER")
                            .allocationPercent(80)
                            .build(),
                    ProjectMemberDTO.builder()
                            .employeeId(200L)
                            .role("TESTER")
                            .allocationPercent(50)
                            .build()
            );

            Set<Long> employeeIds = Set.of(100L, 200L);
            List<EmployeeDTO> validatedEmployees = List.of(testEmployeeDTO);

            List<ProjectMember> savedMembers = List.of(
                    ProjectMember.builder().id(1L).project(testProject).employeeId(100L).build(),
                    ProjectMember.builder().id(2L).project(testProject).employeeId(200L).build()
            );

            List<ProjectMemberDTO> savedMemberDTOs = List.of(
                    ProjectMemberDTO.builder().id(1L).employeeId(100L).build(),
                    ProjectMemberDTO.builder().id(2L).employeeId(200L).build()
            );

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeValidationService.validateEmployeesExist(employeeIds))
                    .thenReturn(validatedEmployees);
            when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 100L)).thenReturn(false);
            when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 200L)).thenReturn(false);
            when(projectMemberRepository.saveAll(any())).thenReturn(savedMembers);
            when(projectMapper.memberToDTO(any(ProjectMember.class)))
                    .thenReturn(savedMemberDTOs.get(0), savedMemberDTOs.get(1));

            // When
            List<ProjectMemberDTO> result = projectService.addProjectMembers(1L, memberRequests);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmployeeId()).isEqualTo(100L);
            assertThat(result.get(1).getEmployeeId()).isEqualTo(200L);
            verify(projectRepository).findById(1L);
            verify(employeeValidationService).validateEmployeesExist(employeeIds);
            verify(projectMemberRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when project does not exist")
        void shouldThrowProjectNotFoundExceptionWhenProjectDoesNotExist() {
            // Given
            List<ProjectMemberDTO> memberRequests = List.of(testProjectMemberDTO);
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.addProjectMembers(999L, memberRequests))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw BusinessConflictException for duplicate employee IDs in request")
        void shouldThrowBusinessConflictExceptionForDuplicateEmployeeIdsInRequest() {
            // Given
            List<ProjectMemberDTO> memberRequests = List.of(
                    ProjectMemberDTO.builder().employeeId(100L).build(),
                    ProjectMemberDTO.builder().employeeId(100L).build() // duplicate
            );

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // When & Then
            assertThatThrownBy(() -> projectService.addProjectMembers(1L, memberRequests))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("Duplicate employee IDs in request");
            verify(projectRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw BusinessConflictException when employee is already a member")
        void shouldThrowBusinessConflictExceptionWhenEmployeeIsAlreadyMember() {
            // Given
            List<ProjectMemberDTO> memberRequests = List.of(testProjectMemberDTO);
            Set<Long> employeeIds = Set.of(100L);
            List<EmployeeDTO> validatedEmployees = List.of(testEmployeeDTO);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeValidationService.validateEmployeesExist(employeeIds))
                    .thenReturn(validatedEmployees);
            when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 100L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> projectService.addProjectMembers(1L, memberRequests))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("One or more employees are already project members");
        }

        @Test
        @DisplayName("Should throw BusinessConflictException when employee validation fails")
        void shouldThrowBusinessConflictExceptionWhenEmployeeValidationFails() {
            // Given
            List<ProjectMemberDTO> memberRequests = List.of(testProjectMemberDTO);
            Set<Long> employeeIds = Set.of(100L);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeValidationService.validateEmployeesExist(employeeIds))
                    .thenThrow(new ExternalServiceNotFoundException("Employee not found", "EmployeeService"));

            // When & Then
            assertThatThrownBy(() -> projectService.addProjectMembers(1L, memberRequests))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("One or more employees not found");
        }
    }

    @Nested
    @DisplayName("removeProjectMember() Tests")
    class RemoveProjectMemberTests {

        @Test
        @DisplayName("Should remove project member successfully")
        void shouldRemoveProjectMemberSuccessfully() {
            // Given
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 100L)).thenReturn(true);

            // When
            projectService.removeProjectMember(1L, 100L);

            // Then
            verify(projectRepository).existsById(1L);
            verify(projectMemberRepository).existsByProject_IdAndEmployeeId(1L, 100L);
            verify(projectMemberRepository).deleteByProject_IdAndEmployeeId(1L, 100L);
        }

        @Test
        @DisplayName("Should throw ProjectNotFoundException when project does not exist")
        void shouldThrowProjectNotFoundExceptionWhenProjectDoesNotExist() {
            // Given
            when(projectRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> projectService.removeProjectMember(999L, 100L))
                    .isInstanceOf(ProjectNotFoundException.class);
            verify(projectRepository).existsById(999L);
        }

        @Test
        @DisplayName("Should throw ProjectMemberNotFoundException when member does not exist")
        void shouldThrowProjectMemberNotFoundExceptionWhenMemberDoesNotExist() {
            // Given
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(projectMemberRepository.existsByProject_IdAndEmployeeId(1L, 999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> projectService.removeProjectMember(1L, 999L))
                    .isInstanceOf(ProjectMemberNotFoundException.class);
            verify(projectRepository).existsById(1L);
            verify(projectMemberRepository).existsByProject_IdAndEmployeeId(1L, 999L);
        }
    }

    @Nested
    @DisplayName("getProjectStats() Tests")
    class GetProjectStatsTests {

        @Test
        @DisplayName("Should return project stats by status")
        void shouldReturnProjectStatsByStatus() {
            // Given
            List<ProjectStatProjection> statusStats = Arrays.asList(
                    createProjectStatProjection("ACTIVE", 5L),
                    createProjectStatProjection("COMPLETED", 3L),
                    createProjectStatProjection("CANCELLED", 2L)
            );

            when(projectRepository.countByStatus()).thenReturn(statusStats);

            // When
            ProjectStatsDTO result = projectService.getProjectStats("status");

            // Then
            assertThat(result.getGroupBy()).isEqualTo("status");
            assertThat(result.getStats()).hasSize(3);
            assertThat(result.getStats().get(0).getLabel()).isEqualTo("ACTIVE");
            assertThat(result.getStats().get(0).getCount()).isEqualTo(5L);
            assertThat(result.getStats().get(0).getDisplayName()).isEqualTo("Active");
            verify(projectRepository).countByStatus();
        }

        @Test
        @DisplayName("Should return project stats by month")
        void shouldReturnProjectStatsByMonth() {
            // Given
            List<ProjectStatProjection> monthStats = Arrays.asList(
                    createProjectStatProjection("2024-01", 3L),
                    createProjectStatProjection("2024-02", 5L)
            );

            when(projectRepository.countByStartMonth()).thenReturn(monthStats);

            // When
            ProjectStatsDTO result = projectService.getProjectStats("month");

            // Then
            assertThat(result.getGroupBy()).isEqualTo("month");
            assertThat(result.getStats()).hasSize(2);
            assertThat(result.getStats().get(0).getLabel()).isEqualTo("2024-01");
            assertThat(result.getStats().get(0).getCount()).isEqualTo(3L);
            assertThat(result.getStats().get(0).getDisplayName()).isEqualTo("January 2024");
            verify(projectRepository).countByStartMonth();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid groupBy parameter")
        void shouldThrowIllegalArgumentExceptionForInvalidGroupByParameter() {
            // When & Then
            assertThatThrownBy(() -> projectService.getProjectStats("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("groupBy must be 'status' or 'month'");
        }

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
}