package com.example.employee.web;

import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeePatchDTO;
import com.example.employee.dto.EmployeeStatsDTO;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@DisplayName("Employee Controller Tests")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmployeeDTO alice;
    private EmployeeDTO bob;
    private EmployeeDTO carla;

    @BeforeEach
    void setUp() {
        DepartmentDTO engineering = DepartmentDTO.builder()
                .id(1L)
                .name("Engineering")
                .description("Builds and maintains products")
                .build();

        DepartmentDTO hr = DepartmentDTO.builder()
                .id(2L)
                .name("HR")
                .description("People operations and recruiting")
                .build();

        alice = EmployeeDTO.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Nguyen")
                .email("alice@example.com")
                .departmentId(1L)
                .department(engineering)
                .build();

        bob = EmployeeDTO.builder()
                .id(2L)
                .firstName("Bob")
                .lastName("Martinez")
                .email("bob@example.com")
                .departmentId(1L)
                .department(engineering)
                .build();

        carla = EmployeeDTO.builder()
                .id(3L)
                .firstName("Carla")
                .lastName("Singh")
                .email("carla@example.com")
                .departmentId(2L)
                .department(hr)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/employees - Paginated Employees")
    class GetAllPaginatedTests {

        @Test
        @DisplayName("Should return first page with default pagination")
        void shouldReturnFirstPageWithDefaults() throws Exception {
            // Given
            List<EmployeeDTO> employees = List.of(alice, bob, carla);
            Page<EmployeeDTO> page = new PageImpl<>(employees, PageRequest.of(0, 20), 3);
            when(employeeService.getAll(0, 20, null, null, null, null)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[0].firstName", is("Alice")))
                    .andExpect(jsonPath("$.content[1].firstName", is("Bob")))
                    .andExpect(jsonPath("$.content[2].firstName", is("Carla")))
                    .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageable.pageSize", is(20)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.first", is(true)))
                    .andExpect(jsonPath("$.last", is(true)));

            verify(employeeService).getAll(0, 20, null, null, null, null);
        }

        @Test
        @DisplayName("Should return second page when requesting page 1 with size 2")
        void shouldReturnSecondPageWithCustomPagination() throws Exception {
            // Given
            List<EmployeeDTO> secondPageEmployees = List.of(carla);
            Page<EmployeeDTO> page = new PageImpl<>(secondPageEmployees, PageRequest.of(1, 2), 3);
            when(employeeService.getAll(1, 2, null, null, null, null)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName", is("Carla")))
                    .andExpect(jsonPath("$.pageable.pageNumber", is(1)))
                    .andExpect(jsonPath("$.pageable.pageSize", is(2)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.totalPages", is(2)))
                    .andExpect(jsonPath("$.first", is(false)))
                    .andExpect(jsonPath("$.last", is(true)));

            verify(employeeService).getAll(1, 2, null, null, null, null);
        }

        @Test
        @DisplayName("Should return empty page when requesting page beyond available data")
        void shouldReturnEmptyPageWhenBeyondAvailableData() throws Exception {
            // Given
            Page<EmployeeDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 3);
            when(employeeService.getAll(1, 10, null, null, null, null)).thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.pageable.pageNumber", is(1)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.first", is(false)))
                    .andExpect(jsonPath("$.last", is(true)))
                    .andExpect(jsonPath("$.empty", is(true)));
        }

        @Test
        @DisplayName("Should apply sorting when sort parameter is provided")
        void shouldApplySortingWhenProvided() throws Exception {
            // Given
            List<EmployeeDTO> sortedEmployees = List.of(alice, bob, carla);
            Page<EmployeeDTO> page = new PageImpl<>(sortedEmployees, PageRequest.of(0, 20, Sort.by("firstName").ascending()), 3);
            when(employeeService.getAll(0, 20, "firstName,asc", null, null, null)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("sort", "firstName,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.pageable.sort.sorted", is(true)));

            verify(employeeService).getAll(0, 20, "firstName,asc", null, null, null);
        }

        @Test
        @DisplayName("Should filter by email when email parameter is provided")
        void shouldFilterByEmail() throws Exception {
            // Given
            List<EmployeeDTO> filteredEmployees = List.of(alice);
            Page<EmployeeDTO> page = new PageImpl<>(filteredEmployees, PageRequest.of(0, 20), 1);
            when(employeeService.getAll(0, 20, null, "alice@example.com", null, null)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("email", "alice@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].email", is("alice@example.com")))
                    .andExpect(jsonPath("$.totalElements", is(1)));

            verify(employeeService).getAll(0, 20, null, "alice@example.com", null, null);
        }

        @Test
        @DisplayName("Should filter by last name containing when lastNameContains parameter is provided")
        void shouldFilterByLastNameContains() throws Exception {
            // Given
            List<EmployeeDTO> filteredEmployees = List.of(bob);
            Page<EmployeeDTO> page = new PageImpl<>(filteredEmployees, PageRequest.of(0, 20), 1);
            when(employeeService.getAll(0, 20, null, null, "Martinez", null)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("lastNameContains", "Martinez"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].lastName", is("Martinez")))
                    .andExpect(jsonPath("$.totalElements", is(1)));

            verify(employeeService).getAll(0, 20, null, null, "Martinez", null);
        }

        @Test
        @DisplayName("Should filter by department ID when departmentId parameter is provided")
        void shouldFilterByDepartmentId() throws Exception {
            // Given
            List<EmployeeDTO> engineeringEmployees = List.of(alice, bob);
            Page<EmployeeDTO> page = new PageImpl<>(engineeringEmployees, PageRequest.of(0, 20), 2);
            when(employeeService.getAll(0, 20, null, null, null, 1L)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("departmentId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].departmentId", is(1)))
                    .andExpect(jsonPath("$.content[1].departmentId", is(1)))
                    .andExpect(jsonPath("$.totalElements", is(2)));

            verify(employeeService).getAll(0, 20, null, null, null, 1L);
        }

        @Test
        @DisplayName("Should apply multiple filters simultaneously")
        void shouldApplyMultipleFilters() throws Exception {
            // Given
            List<EmployeeDTO> filteredEmployees = List.of(alice);
            Page<EmployeeDTO> page = new PageImpl<>(filteredEmployees, PageRequest.of(0, 10), 1);
            when(employeeService.getAll(0, 10, "firstName,desc", null, "Nguyen", 1L)).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/employees")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "firstName,desc")
                            .param("lastNameContains", "Nguyen")
                            .param("departmentId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].firstName", is("Alice")));

            verify(employeeService).getAll(0, 10, "firstName,desc", null, "Nguyen", 1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employees/{id} - Get Employee by ID")
    class GetEmployeeByIdTests {

        @Test
        @DisplayName("Should return employee when valid ID is provided")
        void shouldReturnEmployeeWhenValidId() throws Exception {
            // Given
            when(employeeService.getById(1L, true)).thenReturn(alice);

            // When & Then
            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.firstName", is("Alice")))
                    .andExpect(jsonPath("$.lastName", is("Nguyen")))
                    .andExpect(jsonPath("$.email", is("alice@example.com")))
                    .andExpect(jsonPath("$.departmentId", is(1)))
                    .andExpect(jsonPath("$.department.name", is("Engineering")));

            verify(employeeService).getById(1L, true);
        }

        @Test
        @DisplayName("Should return employee without department when enrichWithDepartment is false")
        void shouldReturnEmployeeWithoutDepartment() throws Exception {
            // Given
            EmployeeDTO aliceWithoutDept = EmployeeDTO.builder()
                    .id(1L)
                    .firstName("Alice")
                    .lastName("Nguyen")
                    .email("alice@example.com")
                    .departmentId(1L)
                    .build();
            when(employeeService.getById(1L, false)).thenReturn(aliceWithoutDept);

            // When & Then
            mockMvc.perform(get("/api/v1/employees/1")
                            .param("enrichWithDepartment", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.firstName", is("Alice")))
                    .andExpect(jsonPath("$.department").doesNotExist());

            verify(employeeService).getById(1L, false);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/employees/ - Create Employee")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Should create employee when valid data is provided")
        void shouldCreateEmployeeWhenValidData() throws Exception {
            // Given
            EmployeeDTO newEmployee = EmployeeDTO.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .departmentId(1L)
                    .build();

            EmployeeDTO createdEmployee = EmployeeDTO.builder()
                    .id(4L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .departmentId(1L)
                    .build();

            when(employeeService.create(any(EmployeeDTO.class), eq("test-key"))).thenReturn(createdEmployee);

            // When & Then
            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "test-key")
                            .content(objectMapper.writeValueAsString(newEmployee)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(4)))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")))
                    .andExpect(jsonPath("$.email", is("john.doe@example.com")));

            verify(employeeService).create(any(EmployeeDTO.class), eq("test-key"));
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            // Given
            EmployeeDTO invalidEmployee = EmployeeDTO.builder()
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidEmployee)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailFormatInvalid() throws Exception {
            // Given
            EmployeeDTO invalidEmployee = EmployeeDTO.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("invalid-email")
                    .departmentId(1L)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidEmployee)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/employees/{id} - Update Employee")
    class UpdateEmployeeTests {

        @Test
        @DisplayName("Should update employee when valid data is provided")
        void shouldUpdateEmployeeWhenValidData() throws Exception {
            // Given
            EmployeeDTO updateData = EmployeeDTO.builder()
                    .firstName("Alice Updated")
                    .lastName("Nguyen Updated")
                    .email("alice.updated@example.com")
                    .departmentId(2L)
                    .build();

            EmployeeDTO updatedEmployee = EmployeeDTO.builder()
                    .id(1L)
                    .firstName("Alice Updated")
                    .lastName("Nguyen Updated")
                    .email("alice.updated@example.com")
                    .departmentId(2L)
                    .build();

            when(employeeService.updateEmployee(eq(1L), any(EmployeeDTO.class))).thenReturn(updatedEmployee);

            // When & Then
            mockMvc.perform(put("/api/v1/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.firstName", is("Alice Updated")))
                    .andExpect(jsonPath("$.lastName", is("Nguyen Updated")))
                    .andExpect(jsonPath("$.email", is("alice.updated@example.com")))
                    .andExpect(jsonPath("$.departmentId", is(2)));

            verify(employeeService).updateEmployee(eq(1L), any(EmployeeDTO.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/employees/{id} - Partially Update Employee")
    class PatchEmployeeTests {

        @Test
        @DisplayName("Should partially update employee when valid patch data is provided")
        void shouldPartiallyUpdateEmployeeWhenValidData() throws Exception {
            // Given
            EmployeePatchDTO patchData = EmployeePatchDTO.builder()
                    .firstName("Alice Patched")
                    .email("alice.patched@example.com")
                    .build();

            EmployeeDTO patchedEmployee = EmployeeDTO.builder()
                    .id(1L)
                    .firstName("Alice Patched")
                    .lastName("Nguyen")
                    .email("alice.patched@example.com")
                    .departmentId(1L)
                    .build();

            when(employeeService.patchEmployee(eq(1L), any(EmployeePatchDTO.class))).thenReturn(patchedEmployee);

            // When & Then
            mockMvc.perform(patch("/api/v1/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.firstName", is("Alice Patched")))
                    .andExpect(jsonPath("$.lastName", is("Nguyen")))
                    .andExpect(jsonPath("$.email", is("alice.patched@example.com")));

            verify(employeeService).patchEmployee(eq(1L), any(EmployeePatchDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/employees/{id} - Delete Employee")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Should delete employee and return 204 when valid ID is provided")
        void shouldDeleteEmployeeWhenValidId() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/employees/1"))
                    .andExpect(status().isNoContent());

            verify(employeeService).deleteEmployee(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employees/search - Search Employees")
    class SearchEmployeesTests {

        @Test
        @DisplayName("Should return search results when query is provided")
        void shouldReturnSearchResultsWhenQueryProvided() throws Exception {
            // Given
            List<EmployeeDTO> searchResults = List.of(alice);
            when(employeeService.searchEmployees("alice")).thenReturn(searchResults);

            // When & Then
            mockMvc.perform(get("/api/v1/employees/search")
                            .param("q", "alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].firstName", is("Alice")));

            verify(employeeService).searchEmployees("alice");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employees/stats - Employee Statistics")
    class EmployeeStatsTests {

        @Test
        @DisplayName("Should return employee statistics")
        void shouldReturnEmployeeStatistics() throws Exception {
            // Given
            EmployeeStatsDTO stats = new EmployeeStatsDTO();
            stats.setTotalEmployees(3);
            stats.setDepartmentsWithEmployees(2);
            stats.setAverageEmployeesPerDepartment(1.5);

            Map<Long, Long> byDeptId = new HashMap<>();
            byDeptId.put(1L, 2L);
            byDeptId.put(2L, 1L);
            stats.setEmployeesByDepartment(byDeptId);

            Map<String, Long> byDeptName = new HashMap<>();
            byDeptName.put("Engineering", 2L);
            byDeptName.put("HR", 1L);
            stats.setEmployeesByDepartmentName(byDeptName);

            when(employeeService.getEmployeeStats()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/v1/employees/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEmployees", is(3)))
                    .andExpect(jsonPath("$.departmentsWithEmployees", is(2)))
                    .andExpect(jsonPath("$.averageEmployeesPerDepartment", is(1.5)))
                    .andExpect(jsonPath("$.employeesByDepartment['1']", is(2)))
                    .andExpect(jsonPath("$.employeesByDepartment['2']", is(1)))
                    .andExpect(jsonPath("$.employeesByDepartmentName.Engineering", is(2)))
                    .andExpect(jsonPath("$.employeesByDepartmentName.HR", is(1)));

            verify(employeeService).getEmployeeStats();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employees/count - Count by Department")
    class CountByDepartmentTests {

        @Test
        @DisplayName("Should return count of employees in department")
        void shouldReturnEmployeeCountInDepartment() throws Exception {
            // Given
            when(employeeService.countByDepartmentId(1L)).thenReturn(2L);

            // When & Then
            mockMvc.perform(get("/api/v1/employees/count")
                            .param("departmentId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2"));

            verify(employeeService).countByDepartmentId(1L);
        }
    }
}