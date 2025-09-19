package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeePatchDTO;
import com.example.employee.dto.EmployeeStatsDTO;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
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
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private DepartmentClient departmentClient;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private EmployeeDTO testEmployeeDTO;
    private DepartmentDTO testDepartmentDTO;

    @BeforeEach
    void setUp() {
        testEmployee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(100L)
                .build();

        testEmployeeDTO = EmployeeDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(100L)
                .build();

        testDepartmentDTO = new DepartmentDTO();
        testDepartmentDTO.setId(100L);
        testDepartmentDTO.setName("Engineering");
        testDepartmentDTO.setDescription("Software Engineering Department");
    }

    @Nested
    @DisplayName("getAll() Tests")
    class GetAllTests {

        @Test
        @DisplayName("Should return all employees as list")
        void shouldReturnAllEmployeesAsList() {
            // Given
            List<Employee> employees = List.of(testEmployee);
            when(repository.findAll()).thenReturn(employees);
            when(departmentClient.getDepartment(100L)).thenReturn(testDepartmentDTO);

            // When
            List<EmployeeDTO> result = employeeService.getAll();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Should return paginated employees with filters")
        void shouldReturnPaginatedEmployeesWithFilters() {
            // Given
            Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));
            when(repository.findWithFilters(anyString(), anyString(), anyLong(), any(Pageable.class)))
                    .thenReturn(employeePage);
            when(departmentClient.getDepartment(100L)).thenReturn(testDepartmentDTO);

            // When
            Page<EmployeeDTO> result = employeeService.getAll(0, 10, "firstName,asc",
                    "john.doe@example.com", "Doe", 100L);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@example.com");
            verify(repository).findWithFilters("john.doe@example.com", "Doe", 100L,
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "firstName")));
        }
    }

    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return employee by ID with department enrichment")
        void shouldReturnEmployeeByIdWithDepartmentEnrichment() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(departmentClient.getDepartment(100L)).thenReturn(testDepartmentDTO);

            // When
            EmployeeDTO result = employeeService.getById(1L, true);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getDepartment()).isNotNull();
            assertThat(result.getDepartment().getName()).isEqualTo("Engineering");
            verify(repository).findById(1L);
            verify(departmentClient).getDepartment(100L);
        }

        @Test
        @DisplayName("Should return employee by ID without department enrichment")
        void shouldReturnEmployeeByIdWithoutDepartmentEnrichment() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));

            // When
            EmployeeDTO result = employeeService.getById(1L, false);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getDepartment()).isNull();
            verify(repository).findById(1L);
            verify(departmentClient, never()).getDepartment(anyLong());
        }

        @Test
        @DisplayName("Should throw EmployeeNotFoundException when employee not found")
        void shouldThrowEmployeeNotFoundExceptionWhenEmployeeNotFound() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> employeeService.getById(999L))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining("999");
            verify(repository).findById(999L);
        }

        @Test
        @DisplayName("Should handle department client failure gracefully")
        void shouldHandleDepartmentClientFailureGracefully() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(departmentClient.getDepartment(100L)).thenThrow(new RuntimeException("Service unavailable"));

            // When
            EmployeeDTO result = employeeService.getById(1L, true);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDepartment()).isNull(); // Should be null due to failure
            verify(repository).findById(1L);
            verify(departmentClient).getDepartment(100L);
        }
    }

    @Nested
    @DisplayName("create() Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create employee successfully without idempotency key")
        void shouldCreateEmployeeSuccessfullyWithoutIdempotencyKey() {
            // Given
            EmployeeDTO newEmployeeDTO = EmployeeDTO.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .departmentId(200L)
                    .build();

            Employee savedEmployee = Employee.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .departmentId(200L)
                    .build();

            when(repository.existsByEmail("jane.smith@example.com")).thenReturn(false);
            when(repository.save(any(Employee.class))).thenReturn(savedEmployee);
            when(departmentClient.getDepartment(200L)).thenReturn(testDepartmentDTO);

            // When
            EmployeeDTO result = employeeService.create(newEmployeeDTO);

            // Then
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
            verify(repository).existsByEmail("jane.smith@example.com");
            verify(repository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should create employee successfully with idempotency key")
        void shouldCreateEmployeeSuccessfullyWithIdempotencyKey() {
            // Given
            String idempotencyKey = "create-123";
            EmployeeDTO newEmployeeDTO = EmployeeDTO.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .departmentId(200L)
                    .build();

            Employee savedEmployee = Employee.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .departmentId(200L)
                    .build();

            when(idempotencyService.isProcessed(idempotencyKey)).thenReturn(false);
            when(repository.existsByEmail("jane.smith@example.com")).thenReturn(false);
            when(repository.save(any(Employee.class))).thenReturn(savedEmployee);
            when(departmentClient.getDepartment(200L)).thenReturn(testDepartmentDTO);

            // When
            EmployeeDTO result = employeeService.create(newEmployeeDTO, idempotencyKey);

            // Then
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
            verify(idempotencyService).isProcessed(idempotencyKey);
            verify(idempotencyService).storeResult(eq(idempotencyKey), any(EmployeeDTO.class));
            verify(repository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should return cached result for duplicate idempotency key")
        void shouldReturnCachedResultForDuplicateIdempotencyKey() {
            // Given
            String idempotencyKey = "create-123";
            EmployeeDTO cachedResult = EmployeeDTO.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .build();

            when(idempotencyService.isProcessed(idempotencyKey)).thenReturn(true);
            when(idempotencyService.getResult(idempotencyKey)).thenReturn(cachedResult);

            // When
            EmployeeDTO result = employeeService.create(testEmployeeDTO, idempotencyKey);

            // Then
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
            verify(idempotencyService).isProcessed(idempotencyKey);
            verify(idempotencyService).getResult(idempotencyKey);
            verify(repository, never()).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should throw DuplicateEmployeeException for duplicate email")
        void shouldThrowDuplicateEmployeeExceptionForDuplicateEmail() {
            // Given
            when(repository.existsByEmail("john.doe@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> employeeService.create(testEmployeeDTO))
                    .isInstanceOf(DuplicateEmployeeException.class)
                    .hasMessageContaining("john.doe@example.com");
            verify(repository).existsByEmail("john.doe@example.com");
            verify(repository, never()).save(any(Employee.class));
        }
    }

    @Nested
    @DisplayName("updateEmployee() Tests")
    class UpdateEmployeeTests {

        @Test
        @DisplayName("Should update employee successfully")
        void shouldUpdateEmployeeSuccessfully() {
            // Given
            EmployeeDTO updateDTO = EmployeeDTO.builder()
                    .firstName("John Updated")
                    .lastName("Doe Updated")
                    .email("john.updated@example.com")
                    .departmentId(300L)
                    .build();

            Employee updatedEmployee = Employee.builder()
                    .id(1L)
                    .firstName("John Updated")
                    .lastName("Doe Updated")
                    .email("john.updated@example.com")
                    .departmentId(300L)
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(repository.existsByEmailAndIdNot("john.updated@example.com", 1L)).thenReturn(false);
            when(repository.save(any(Employee.class))).thenReturn(updatedEmployee);
            when(departmentClient.getDepartment(300L)).thenReturn(testDepartmentDTO);

            // When
            EmployeeDTO result = employeeService.updateEmployee(1L, updateDTO);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("John Updated");
            assertThat(result.getEmail()).isEqualTo("john.updated@example.com");
            verify(repository).findById(1L);
            verify(repository).existsByEmailAndIdNot("john.updated@example.com", 1L);
            verify(repository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should throw EmployeeNotFoundException when updating non-existent employee")
        void shouldThrowEmployeeNotFoundExceptionWhenUpdatingNonExistentEmployee() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> employeeService.updateEmployee(999L, testEmployeeDTO))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining("999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should throw DuplicateEmployeeException when updating to duplicate email")
        void shouldThrowDuplicateEmployeeExceptionWhenUpdatingToDuplicateEmail() {
            // Given
            EmployeeDTO updateDTO = EmployeeDTO.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("existing@example.com")
                    .departmentId(100L)
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(repository.existsByEmailAndIdNot("existing@example.com", 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> employeeService.updateEmployee(1L, updateDTO))
                    .isInstanceOf(DuplicateEmployeeException.class)
                    .hasMessageContaining("existing@example.com");
            verify(repository).findById(1L);
            verify(repository).existsByEmailAndIdNot("existing@example.com", 1L);
            verify(repository, never()).save(any(Employee.class));
        }
    }

    @Nested
    @DisplayName("patchEmployee() Tests")
    class PatchEmployeeTests {

        @Test
        @DisplayName("Should patch employee with partial updates")
        void shouldPatchEmployeeWithPartialUpdates() {
            // Given
            EmployeePatchDTO patchDTO = new EmployeePatchDTO();
            patchDTO.setFirstName("John Updated");
            patchDTO.setEmail("john.patched@example.com");
            // lastName and departmentId remain null (no change)

            Employee patchedEmployee = Employee.builder()
                    .id(1L)
                    .firstName("John Updated")
                    .lastName("Doe") // unchanged
                    .email("john.patched@example.com")
                    .departmentId(100L) // unchanged
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(repository.existsByEmailAndIdNot("john.patched@example.com", 1L)).thenReturn(false);
            when(repository.save(any(Employee.class))).thenReturn(patchedEmployee);
            when(departmentClient.getDepartment(100L)).thenReturn(testDepartmentDTO);

            // When
            EmployeeDTO result = employeeService.patchEmployee(1L, patchDTO);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("John Updated");
            assertThat(result.getLastName()).isEqualTo("Doe"); // unchanged
            assertThat(result.getEmail()).isEqualTo("john.patched@example.com");
            verify(repository).findById(1L);
            verify(repository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should throw EmployeeNotFoundException when patching non-existent employee")
        void shouldThrowEmployeeNotFoundExceptionWhenPatchingNonExistentEmployee() {
            // Given
            EmployeePatchDTO patchDTO = new EmployeePatchDTO();
            patchDTO.setFirstName("Updated Name");

            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> employeeService.patchEmployee(999L, patchDTO))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining("999");
            verify(repository).findById(999L);
        }

        @Test
        @DisplayName("Should throw DuplicateEmployeeException when patching to duplicate email")
        void shouldThrowDuplicateEmployeeExceptionWhenPatchingToDuplicateEmail() {
            // Given
            EmployeePatchDTO patchDTO = new EmployeePatchDTO();
            patchDTO.setEmail("existing@example.com");

            when(repository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(repository.existsByEmailAndIdNot("existing@example.com", 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> employeeService.patchEmployee(1L, patchDTO))
                    .isInstanceOf(DuplicateEmployeeException.class)
                    .hasMessageContaining("existing@example.com");
            verify(repository).findById(1L);
            verify(repository).existsByEmailAndIdNot("existing@example.com", 1L);
        }
    }

    @Nested
    @DisplayName("deleteEmployee() Tests")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Should delete employee successfully")
        void shouldDeleteEmployeeSuccessfully() {
            // Given
            when(repository.existsById(1L)).thenReturn(true);

            // When
            employeeService.deleteEmployee(1L);

            // Then
            verify(repository).existsById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw EmployeeNotFoundException when deleting non-existent employee")
        void shouldThrowEmployeeNotFoundExceptionWhenDeletingNonExistentEmployee() {
            // Given
            when(repository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> employeeService.deleteEmployee(999L))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining("999");
            verify(repository).existsById(999L);
            verify(repository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("searchEmployees() Tests")
    class SearchEmployeesTests {

        @Test
        @DisplayName("Should return search results for valid search term")
        void shouldReturnSearchResultsForValidSearchTerm() {
            // Given
            String searchTerm = "john";
            List<Employee> searchResults = List.of(testEmployee);
            when(repository.searchByNameOrEmail("john")).thenReturn(searchResults);
            when(departmentClient.getDepartment(100L)).thenReturn(testDepartmentDTO);

            // When
            List<EmployeeDTO> result = employeeService.searchEmployees(searchTerm);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
            verify(repository).searchByNameOrEmail("john");
        }

        @Test
        @DisplayName("Should return empty list for null search term")
        void shouldReturnEmptyListForNullSearchTerm() {
            // When
            List<EmployeeDTO> result = employeeService.searchEmployees(null);

            // Then
            assertThat(result).isEmpty();
            verify(repository, never()).searchByNameOrEmail(anyString());
        }

        @Test
        @DisplayName("Should return empty list for empty search term")
        void shouldReturnEmptyListForEmptySearchTerm() {
            // When
            List<EmployeeDTO> result = employeeService.searchEmployees("   ");

            // Then
            assertThat(result).isEmpty();
            verify(repository, never()).searchByNameOrEmail(anyString());
        }
    }

    @Nested
    @DisplayName("getEmployeeStats() Tests")
    class GetEmployeeStatsTests {

        @Test
        @DisplayName("Should return employee statistics successfully")
        void shouldReturnEmployeeStatsSuccessfully() {
            // Given
            when(repository.count()).thenReturn(10L);
            when(repository.countByDepartment()).thenReturn(Arrays.asList(
                    new Object[]{100L, 5L},
                    new Object[]{200L, 5L}
            ));
            when(repository.countDistinctDepartments()).thenReturn(2L);

            DepartmentDTO dept1 = new DepartmentDTO();
            dept1.setId(100L);
            dept1.setName("Engineering");

            DepartmentDTO dept2 = new DepartmentDTO();
            dept2.setId(200L);
            dept2.setName("Marketing");

            when(departmentClient.getDepartment(100L)).thenReturn(dept1);
            when(departmentClient.getDepartment(200L)).thenReturn(dept2);

            // When
            EmployeeStatsDTO result = employeeService.getEmployeeStats();

            // Then
            assertThat(result.getTotalEmployees()).isEqualTo(10L);
            assertThat(result.getDepartmentsWithEmployees()).isEqualTo(2L);
            assertThat(result.getAverageEmployeesPerDepartment()).isEqualTo(5.0);
            assertThat(result.getEmployeesByDepartment()).containsEntry(100L, 5L);
            assertThat(result.getEmployeesByDepartment()).containsEntry(200L, 5L);
            assertThat(result.getEmployeesByDepartmentName()).containsEntry("Engineering", 5L);
            assertThat(result.getEmployeesByDepartmentName()).containsEntry("Marketing", 5L);

            verify(repository).count();
            verify(repository).countByDepartment();
            verify(repository).countDistinctDepartments();
        }

        @Test
        @DisplayName("Should handle department client failures gracefully in stats")
        void shouldHandleDepartmentClientFailuresGracefullyInStats() {
            // Given
            when(repository.count()).thenReturn(5L);
            when(repository.countByDepartment()).thenReturn(List.of(
                    new Object[]{100L, 5L},
                    new Object[]{200L, 5L}
            ));
            when(repository.countDistinctDepartments()).thenReturn(1L);
            when(departmentClient.getDepartment(100L)).thenThrow(new RuntimeException("Service unavailable"));

            // When
            EmployeeStatsDTO result = employeeService.getEmployeeStats();

            // Then
            assertThat(result.getTotalEmployees()).isEqualTo(5L);
            assertThat(result.getEmployeesByDepartmentName()).containsEntry("Department 100", 5L);
            verify(departmentClient).getDepartment(100L);
        }
    }

    @Nested
    @DisplayName("countByDepartmentId() Tests")
    class CountByDepartmentIdTests {

        @Test
        @DisplayName("Should return count of employees in department")
        void shouldReturnCountOfEmployeesInDepartment() {
            // Given
            when(repository.countByDepartmentId(100L)).thenReturn(5L);

            // When
            long result = employeeService.countByDepartmentId(100L);

            // Then
            assertThat(result).isEqualTo(5L);
            verify(repository).countByDepartmentId(100L);
        }
    }

    @Nested
    @DisplayName("getEmployeesByDepartment() Tests")
    class GetEmployeesByDepartmentTests {

        @Test
        @DisplayName("Should return employees by department with pagination")
        void shouldReturnEmployeesByDepartmentWithPagination() {
            // Given
            Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));
            when(repository.findWithFilters(isNull(), isNull(), eq(100L), any(Pageable.class)))
                    .thenReturn(employeePage);
            when(departmentClient.getDepartment(100L)).thenReturn(testDepartmentDTO);

            // When
            Page<EmployeeDTO> result = employeeService.getEmployeesByDepartment(100L, 0, 10, "firstName,asc");

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDepartmentId()).isEqualTo(100L);
            verify(repository).findWithFilters(isNull(), isNull(), eq(100L), any(Pageable.class));
        }
    }
}