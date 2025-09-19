package com.example.department.service.impl;

import com.example.department.client.EmployeeClient;
import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import com.example.department.dto.EmployeeDTO;
import com.example.department.exception.DepartmentInUseException;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.repo.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService Unit Tests")
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository repository;

    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department sampleDepartment;
    private DepartmentDTO sampleDepartmentDTO;

    @BeforeEach
    void setUp() {
        sampleDepartment = Department.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Software Engineering Department")
                .build();

        sampleDepartmentDTO = DepartmentDTO.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Software Engineering Department")
                .build();
    }

    // ========================================
    // FIND ALL TESTS (Paginated)
    // ========================================
    @Nested
    @DisplayName("Find All Departments (Paginated)")
    class FindAllPaginatedTests {

        @Test
        @DisplayName("Should return paginated departments with filters")
        void shouldReturnPaginatedDepartmentsWithFilters() {
            // Arrange
            Page<Department> departmentPage = new PageImpl<>(
                    List.of(sampleDepartment),
                    PageRequest.of(0, 10),
                    1
            );

            when(repository.findWithFilters(eq("Eng"), eq("EN"), any(Pageable.class)))
                    .thenReturn(departmentPage);

            // Act
            Page<DepartmentDTO> result = departmentService.findAll(0, 10, "name,asc", "Eng", "EN");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Engineering");
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(repository).findWithFilters("Eng", "EN", PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")));
        }

        @Test
        @DisplayName("Should handle sorting parameters correctly")
        void shouldHandleSortingParametersCorrectly() {
            // Arrange
            Page<Department> departmentPage = new PageImpl<>(List.of(sampleDepartment));
            when(repository.findWithFilters(any(), any(), any(Pageable.class))).thenReturn(departmentPage);

            // Act
            departmentService.findAll(0, 10, "name,desc", null, null);

            // Assert
            verify(repository).findWithFilters(
                    eq(null),
                    eq(null),
                    eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name")))
            );
        }
    }

    // ========================================
    // FIND ALL TESTS (Legacy)
    // ========================================
    @Nested
    @DisplayName("Find All Departments (Legacy)")
    class FindAllLegacyTests {

        @Test
        @DisplayName("Should return all departments without pagination")
        void shouldReturnAllDepartmentsWithoutPagination() {
            // Arrange
            List<Department> departments = List.of(sampleDepartment);
            when(repository.findAll()).thenReturn(departments);

            // Act
            List<Department> result = departmentService.findAll();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(sampleDepartment);
            verify(repository).findAll();
        }
    }

    // ========================================
    // FIND BY ID TESTS
    // ========================================
    @Nested
    @DisplayName("Find Department by ID")
    class FindByIdTests {

        @Test
        @DisplayName("Should return department when found")
        void shouldReturnDepartmentWhenFound() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));

            // Act
            DepartmentDTO result = departmentService.findById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Engineering");
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void shouldThrowExceptionWhenDepartmentNotFound() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> departmentService.findById(999L))
                    .isInstanceOf(DepartmentNotFoundException.class);

            verify(repository).findById(999L);
        }
    }

    // ========================================
    // CREATE TESTS
    // ========================================
    @Nested
    @DisplayName("Create Department")
    class CreateTests {

        @Test
        @DisplayName("Should create department successfully")
        void shouldCreateDepartmentSuccessfully() {
            // Arrange
            DepartmentDTO createDto = DepartmentDTO.builder()
                    .name("HR")
                    .code("HR")
                    .description("Human Resources")
                    .build();

            Department savedDepartment = Department.builder()
                    .id(2L)
                    .name("HR")
                    .code("HR")
                    .description("Human Resources")
                    .build();

            when(repository.existsByName("HR")).thenReturn(false);
            when(repository.existsByCode("HR")).thenReturn(false);
            when(repository.save(any(Department.class))).thenReturn(savedDepartment);

            // Act
            DepartmentDTO result = departmentService.create(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("HR");

            verify(repository).existsByName("HR");
            verify(repository).existsByCode("HR");
            verify(repository).save(any(Department.class));
        }

        @Test
        @DisplayName("Should throw exception when name already exists")
        void shouldThrowExceptionWhenNameAlreadyExists() {
            // Arrange
            DepartmentDTO createDto = DepartmentDTO.builder()
                    .name("Engineering")
                    .code("ENG2")
                    .build();

            when(repository.existsByName("Engineering")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> departmentService.create(createDto))
                    .isInstanceOf(DuplicateDepartmentException.class)
                    .hasMessageContaining("name");

            verify(repository).existsByName("Engineering");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when code already exists")
        void shouldThrowExceptionWhenCodeAlreadyExists() {
            // Arrange
            DepartmentDTO createDto = DepartmentDTO.builder()
                    .name("Engineering 2")
                    .code("ENG")
                    .build();

            when(repository.existsByName("Engineering 2")).thenReturn(false);
            when(repository.existsByCode("ENG")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> departmentService.create(createDto))
                    .isInstanceOf(DuplicateDepartmentException.class)
                    .hasMessageContaining("code");

            verify(repository).existsByCode("ENG");
            verify(repository, never()).save(any());
        }
    }

    // ========================================
    // UPDATE TESTS
    // ========================================
    @Nested
    @DisplayName("Update Department")
    class UpdateTests {

        @Test
        @DisplayName("Should update department successfully")
        void shouldUpdateDepartmentSuccessfully() {
            // Arrange
            DepartmentDTO updateDto = DepartmentDTO.builder()
                    .name("Software Engineering")
                    .code("SENG")
                    .description("Updated description")
                    .build();

            Department updatedDepartment = Department.builder()
                    .id(1L)
                    .name("Software Engineering")
                    .code("SENG")
                    .description("Updated description")
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(repository.existsByNameAndIdNot("Software Engineering", 1L)).thenReturn(false);
            when(repository.existsByCodeAndIdNot("SENG", 1L)).thenReturn(false);
            when(repository.save(any(Department.class))).thenReturn(updatedDepartment);

            // Act
            DepartmentDTO result = departmentService.updateDepartment(1L, updateDto);

            // Assert
            assertThat(result.getName()).isEqualTo("Software Engineering");
            assertThat(result.getCode()).isEqualTo("SENG");

            verify(repository).findById(1L);
            verify(repository).save(any(Department.class));
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void shouldThrowExceptionWhenDepartmentNotFoundForUpdate() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> departmentService.updateDepartment(999L, sampleDepartmentDTO))
                    .isInstanceOf(DepartmentNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    // ========================================
    // PATCH TESTS
    // ========================================
    @Nested
    @DisplayName("Patch Department")
    class PatchTests {

        @Test
        @DisplayName("Should patch only provided fields")
        void shouldPatchOnlyProvidedFields() {
            // Arrange
            DepartmentPatchDTO patchDto = DepartmentPatchDTO.builder()
                    .name("Updated Engineering")
                    .build(); // Only name provided, code and description should remain unchanged

            Department patchedDepartment = Department.builder()
                    .id(1L)
                    .name("Updated Engineering")
                    .code("ENG") // unchanged
                    .description("Software Engineering Department") // unchanged
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(repository.existsByNameAndIdNot("Updated Engineering", 1L)).thenReturn(false);
            when(repository.save(any(Department.class))).thenReturn(patchedDepartment);

            // Act
            DepartmentDTO result = departmentService.patchDepartment(1L, patchDto);

            // Assert
            assertThat(result.getName()).isEqualTo("Updated Engineering");
            assertThat(result.getCode()).isEqualTo("ENG"); // unchanged
            assertThat(result.getDescription()).isEqualTo("Software Engineering Department"); // unchanged

            verify(repository).findById(1L);
            verify(repository).existsByNameAndIdNot("Updated Engineering", 1L);
            verify(repository, never()).existsByCodeAndIdNot(any(), any()); // code not provided
        }

        @Test
        @DisplayName("Should handle null fields in patch")
        void shouldHandleNullFieldsInPatch() {
            // Arrange
            DepartmentPatchDTO patchDto = DepartmentPatchDTO.builder().build(); // All fields null

            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(repository.save(sampleDepartment)).thenReturn(sampleDepartment);

            // Act
            DepartmentDTO result = departmentService.patchDepartment(1L, patchDto);

            // Assert
            assertThat(result.getName()).isEqualTo("Engineering"); // unchanged
            verify(repository).findById(1L);
            verify(repository).save(sampleDepartment);
            // No uniqueness checks should be called
            verify(repository, never()).existsByNameAndIdNot(any(), any());
            verify(repository, never()).existsByCodeAndIdNot(any(), any());
        }
    }

    // ========================================
    // DELETE TESTS
    // ========================================
    @Nested
    @DisplayName("Delete Department")
    class DeleteTests {

        @Test
        @DisplayName("Should delete department when no employees assigned")
        void shouldDeleteDepartmentWhenNoEmployeesAssigned() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(employeeClient.countByDepartmentId(1L)).thenReturn(0L);

            // Act
            departmentService.deleteDepartment(1L);

            // Assert
            verify(repository).findById(1L);
            verify(employeeClient).countByDepartmentId(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when employees are assigned")
        void shouldThrowExceptionWhenEmployeesAreAssigned() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(employeeClient.countByDepartmentId(1L)).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
                    .isInstanceOf(DepartmentInUseException.class);

            verify(repository).findById(1L);
            verify(employeeClient).countByDepartmentId(1L);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void shouldThrowExceptionWhenDepartmentNotFoundForDelete() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> departmentService.deleteDepartment(999L))
                    .isInstanceOf(DepartmentNotFoundException.class);

            verify(repository).findById(999L);
            verify(employeeClient, never()).countByDepartmentId(any());
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should handle employee service failure gracefully")
        void shouldHandleEmployeeServiceFailureGracefully() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(employeeClient.countByDepartmentId(1L)).thenThrow(new RuntimeException("Service unavailable"));

            // Act & Assert
            assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Employee service");

            verify(repository, never()).deleteById(any());
        }
    }

    // ========================================
    // FIND BY CODE TESTS
    // ========================================
    @Nested
    @DisplayName("Find Department by Code")
    class FindByCodeTests {

        @Test
        @DisplayName("Should find department by code")
        void shouldFindDepartmentByCode() {
            // Arrange
            when(repository.findByCode("ENG")).thenReturn(Optional.of(sampleDepartment));

            // Act
            DepartmentDTO result = departmentService.findByCode("eng"); // lowercase input

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("ENG");
            verify(repository).findByCode("ENG"); // should be normalized to uppercase
        }

        @Test
        @DisplayName("Should throw exception when code not found")
        void shouldThrowExceptionWhenCodeNotFound() {
            // Arrange
            when(repository.findByCode("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> departmentService.findByCode("INVALID"))
                    .isInstanceOf(DepartmentNotFoundException.class)
                    .hasMessageContaining("INVALID");
        }

        @Test
        @DisplayName("Should throw exception when code is null or empty")
        void shouldThrowExceptionWhenCodeIsNullOrEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> departmentService.findByCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");

            assertThatThrownBy(() -> departmentService.findByCode("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");

            verify(repository, never()).findByCode(any());
        }
    }

    // ========================================
    // GET DEPARTMENT WITH EMPLOYEES TESTS
    // ========================================
    @Nested
    @DisplayName("Get Department with Employees")
    class GetDepartmentWithEmployeesTests {

        @Test
        @DisplayName("Should return department with employees successfully")
        void shouldReturnDepartmentWithEmployeesSuccessfully() {
            // Arrange
            EmployeeDTO employee1 = EmployeeDTO.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .departmentId(1L)
                    .build();

            EmployeeDTO employee2 = EmployeeDTO.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .departmentId(1L)
                    .build();

            Page<EmployeeDTO> employeePage = new PageImpl<>(
                    Arrays.asList(employee1, employee2),
                    PageRequest.of(0, 10),
                    2
            );

            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(employeeClient.getEmployeesByDepartment(1L, 0, 10, "firstName,asc"))
                    .thenReturn(employeePage);

            // Act
            DepartmentEmployeesDTO result = departmentService.getDepartmentWithEmployees(1L, 0, 10, "firstName,asc");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDepartment().getName()).isEqualTo("Engineering");
            assertThat(result.getEmployees().getContent()).hasSize(2);
            assertThat(result.getTotalEmployees()).isEqualTo(2);
            assertThat(result.getSummary()).contains("Engineering").contains("2 employees");

            verify(repository).findById(1L);
            verify(employeeClient).getEmployeesByDepartment(1L, 0, 10, "firstName,asc");
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void shouldThrowExceptionWhenDepartmentNotFoundForEmployees() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> departmentService.getDepartmentWithEmployees(999L, 0, 10, null))
                    .isInstanceOf(DepartmentNotFoundException.class);

            verify(employeeClient, never()).getEmployeesByDepartment(any(), anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("Should handle employee service failure")
        void shouldHandleEmployeeServiceFailure() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
            when(employeeClient.getEmployeesByDepartment(1L, 0, 10, null))
                    .thenThrow(new RuntimeException("Employee service unavailable"));

            // Act & Assert
            assertThatThrownBy(() -> departmentService.getDepartmentWithEmployees(1L, 0, 10, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Employee service");

            verify(repository).findById(1L);
            verify(employeeClient).getEmployeesByDepartment(1L, 0, 10, null);
        }
    }
}