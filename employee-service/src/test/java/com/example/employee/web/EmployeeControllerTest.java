//package com.example.employee.web;
//
//import com.example.employee.dto.DepartmentDTO;
//import com.example.employee.dto.EmployeeDTO;
//import com.example.employee.dto.EmployeePatchDTO;
//import com.example.employee.dto.EmployeeStatsDTO;
//import com.example.employee.exception.DuplicateEmployeeException;
//import com.example.employee.exception.EmployeeNotFoundException;
//import com.example.employee.service.EmployeeService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.hamcrest.Matchers.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyBoolean;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.ArgumentMatchers.isNull;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(EmployeeController.class)
//@DisplayName("Employee Controller Web Tests")
//class EmployeeControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private EmployeeService employeeService;
//
//    private EmployeeDTO sampleEmployee;
//    private EmployeeDTO sampleEmployee2;
//    private DepartmentDTO sampleDepartment;
//
//    @BeforeEach
//    void setUp() {
//        sampleDepartment = DepartmentDTO.builder()
//                .id(1L)
//                .name("Engineering")
//                .description("Software Development Team")
//                .build();
//
//        sampleEmployee = EmployeeDTO.builder()
//                .id(1L)
//                .firstName("John")
//                .lastName("Doe")
//                .email("john.doe@example.com")
//                .departmentId(1L)
//                .department(sampleDepartment)
//                .build();
//
//        sampleEmployee2 = EmployeeDTO.builder()
//                .id(2L)
//                .firstName("Jane")
//                .lastName("Smith")
//                .email("jane.smith@example.com")
//                .departmentId(1L)
//                .department(sampleDepartment)
//                .build();
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/employees - Get All Employees (Paged)")
//    class GetAllEmployeesTests {
//
//        @Test
//        @DisplayName("Should return paged employees with default parameters")
//        void shouldReturnPagedEmployeesWithDefaults() throws Exception {
//            // Given
//            List<EmployeeDTO> employees = Arrays.asList(sampleEmployee, sampleEmployee2);
//            Page<EmployeeDTO> page = new PageImpl<>(employees);
//            when(employeeService.getAll(0, 20, null, null, null, null)).thenReturn(page);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.content", hasSize(2)))
//                    .andExpect(jsonPath("$.content[0].id", is(1)))
//                    .andExpect(jsonPath("$.content[0].firstName", is("John")))
//                    .andExpect(jsonPath("$.content[0].lastName", is("Doe")))
//                    .andExpect(jsonPath("$.content[0].email", is("john.doe@example.com")))
//                    .andExpect(jsonPath("$.content[0].departmentId", is(1)))
//                    .andExpect(jsonPath("$.content[0].department.name", is("Engineering")))
//                    .andExpect(jsonPath("$.content[1].id", is(2)))
//                    .andExpect(jsonPath("$.content[1].firstName", is("Jane")));
//
//            verify(employeeService).getAll(0, 20, null, null, null, null);
//        }
//
//        @Test
//        @DisplayName("Should return paged employees with custom parameters")
//        void shouldReturnPagedEmployeesWithCustomParams() throws Exception {
//            // Given
//            List<EmployeeDTO> employees = Arrays.asList(sampleEmployee);
//            Page<EmployeeDTO> page = new PageImpl<>(employees);
//            when(employeeService.getAll(1, 10, "lastName,desc", "john.doe@example.com", "Doe", 1L))
//                    .thenReturn(page);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees")
//                            .param("page", "1")
//                            .param("size", "10")
//                            .param("sort", "lastName,desc")
//                            .param("email", "john.doe@example.com")
//                            .param("lastNameContains", "Doe")
//                            .param("departmentId", "1"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.content", hasSize(1)))
//                    .andExpect(jsonPath("$.content[0].email", is("john.doe@example.com")));
//
//            verify(employeeService).getAll(1, 10, "lastName,desc", "john.doe@example.com", "Doe", 1L);
//        }
//
//        @Test
//        @DisplayName("Should return empty page when no employees found")
//        void shouldReturnEmptyPageWhenNoEmployees() throws Exception {
//            // Given
//            Page<EmployeeDTO> emptyPage = new PageImpl<>(Arrays.asList());
//            when(employeeService.getAll(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(emptyPage);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.content", hasSize(0)))
//                    .andExpect(jsonPath("$.totalElements", is(0)));
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/employees/{id} - Get Employee By ID")
//    class GetEmployeeByIdTests {
//
//        @Test
//        @DisplayName("Should return employee when found with department enrichment")
//        void shouldReturnEmployeeWhenFoundWithEnrichment() throws Exception {
//            // Given
//            when(employeeService.getById(1L, true)).thenReturn(sampleEmployee);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/1")
//                            .param("enrichWithDepartment", "true"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.id", is(1)))
//                    .andExpect(jsonPath("$.firstName", is("John")))
//                    .andExpect(jsonPath("$.lastName", is("Doe")))
//                    .andExpect(jsonPath("$.email", is("john.doe@example.com")))
//                    .andExpect(jsonPath("$.departmentId", is(1)))
//                    .andExpect(jsonPath("$.department.name", is("Engineering")));
//
//            verify(employeeService).getById(1L, true);
//        }
//
//        @Test
//        @DisplayName("Should return employee without department when enrichment disabled")
//        void shouldReturnEmployeeWithoutDepartment() throws Exception {
//            // Given
//            EmployeeDTO employeeWithoutDept = EmployeeDTO.builder()
//                    .id(1L)
//                    .firstName("John")
//                    .lastName("Doe")
//                    .email("john.doe@example.com")
//                    .departmentId(1L)
//                    .department(null) // No department enrichment
//                    .build();
//            when(employeeService.getById(1L, false)).thenReturn(employeeWithoutDept);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/1")
//                            .param("enrichWithDepartment", "false"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.department").doesNotExist());
//
//            verify(employeeService).getById(1L, false);
//        }
//
//        @Test
//        @DisplayName("Should use default enrichment when parameter not provided")
//        void shouldUseDefaultEnrichment() throws Exception {
//            // Given
//            when(employeeService.getById(1L, true)).thenReturn(sampleEmployee);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/1"))
//                    .andExpect(status().isOk());
//
//            verify(employeeService).getById(1L, true); // Default is true
//        }
//
//        @Test
//        @DisplayName("Should return 404 when employee not found")
//        void shouldReturn404WhenEmployeeNotFound() throws Exception {
//            // Given
//            when(employeeService.getById(999L, true))
//                    .thenThrow(new EmployeeNotFoundException("999"));
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/999"))
//                    .andExpect(status().isNotFound());
//
//            verify(employeeService).getById(999L, true);
//        }
//    }
//
//    @Nested
//    @DisplayName("POST /api/v1/employees - Create Employee")
//    class CreateEmployeeTests {
//
//        @Test
//        @DisplayName("Should create employee successfully")
//        void shouldCreateEmployeeSuccessfully() throws Exception {
//            // Given
//            EmployeeDTO createRequest = EmployeeDTO.builder()
//                    .firstName("John")
//                    .lastName("Doe")
//                    .email("john.doe@example.com")
//                    .departmentId(1L)
//                    .build();
//
//            when(employeeService.create(any(EmployeeDTO.class), isNull())).thenReturn(sampleEmployee);
//
//            // When & Then
//            mockMvc.perform(post("/api/v1/employees")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(createRequest)))
//                    .andDo(print())
//                    .andExpect(status().isCreated())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.id", is(1)))
//                    .andExpect(jsonPath("$.firstName", is("John")))
//                    .andExpect(jsonPath("$.lastName", is("Doe")))
//                    .andExpect(jsonPath("$.email", is("john.doe@example.com")));
//
//            verify(employeeService).create(any(EmployeeDTO.class), isNull());
//        }
//
//        @Test
//        @DisplayName("Should create employee with idempotency key")
//        void shouldCreateEmployeeWithIdempotencyKey() throws Exception {
//            // Given
//            EmployeeDTO createRequest = EmployeeDTO.builder()
//                    .firstName("John")
//                    .lastName("Doe")
//                    .email("john.doe@example.com")
//                    .departmentId(1L)
//                    .build();
//
//            when(employeeService.create(any(EmployeeDTO.class), eq("abc-123"))).thenReturn(sampleEmployee);
//
//            // When & Then
//            mockMvc.perform(post("/api/v1/employees")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .header("Idempotency-Key", "abc-123")
//                            .content(objectMapper.writeValueAsString(createRequest)))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.id", is(1)));
//
//            verify(employeeService).create(any(EmployeeDTO.class), eq("abc-123"));
//        }
//
//        @Test
//        @DisplayName("Should return 400 for invalid employee data")
//        void shouldReturn400ForInvalidEmployeeData() throws Exception {
//            // Given
//            EmployeeDTO invalidRequest = EmployeeDTO.builder()
//                    .firstName("") // Invalid - empty
//                    .lastName("Doe")
//                    .email("invalid-email") // Invalid email format
//                    .departmentId(1L)
//                    .build();
//
//            // When & Then
//            mockMvc.perform(post("/api/v1/employees")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidRequest)))
//                    .andExpect(status().isBadRequest());
//
//            verify(employeeService, never()).create(any(), any());
//        }
//
//        @Test
//        @DisplayName("Should return 409 when employee email already exists")
//        void shouldReturn409WhenEmailExists() throws Exception {
//            // Given
//            EmployeeDTO createRequest = EmployeeDTO.builder()
//                    .firstName("John")
//                    .lastName("Doe")
//                    .email("john.doe@example.com")
//                    .departmentId(1L)
//                    .build();
//
//            when(employeeService.create(any(EmployeeDTO.class), any()))
//                    .thenThrow(new DuplicateEmployeeException("email", "john.doe@example.com"));
//
//            // When & Then
//            mockMvc.perform(post("/api/v1/employees")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(createRequest)))
//                    .andExpect(status().isConflict());
//
//            verify(employeeService).create(any(EmployeeDTO.class), any());
//        }
//    }
//
//    @Nested
//    @DisplayName("PUT /api/v1/employees/{id} - Update Employee")
//    class UpdateEmployeeTests {
//
//        @Test
//        @DisplayName("Should update employee successfully")
//        void shouldUpdateEmployeeSuccessfully() throws Exception {
//            // Given
//            EmployeeDTO updateRequest = EmployeeDTO.builder()
//                    .firstName("John Updated")
//                    .lastName("Doe Updated")
//                    .email("john.updated@example.com")
//                    .departmentId(2L)
//                    .build();
//
//            EmployeeDTO updatedEmployee = EmployeeDTO.builder()
//                    .id(1L)
//                    .firstName("John Updated")
//                    .lastName("Doe Updated")
//                    .email("john.updated@example.com")
//                    .departmentId(2L)
//                    .build();
//
//            when(employeeService.updateEmployee(eq(1L), any(EmployeeDTO.class))).thenReturn(updatedEmployee);
//
//            // When & Then
//            mockMvc.perform(put("/api/v1/employees/1")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateRequest)))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id", is(1)))
//                    .andExpect(jsonPath("$.firstName", is("John Updated")))
//                    .andExpect(jsonPath("$.lastName", is("Doe Updated")))
//                    .andExpect(jsonPath("$.email", is("john.updated@example.com")))
//                    .andExpect(jsonPath("$.departmentId", is(2)));
//
//            verify(employeeService).updateEmployee(eq(1L), any(EmployeeDTO.class));
//        }
//
//        @Test
//        @DisplayName("Should return 404 when updating non-existent employee")
//        void shouldReturn404WhenUpdatingNonExistentEmployee() throws Exception {
//            // Given
//            EmployeeDTO updateRequest = EmployeeDTO.builder()
//                    .firstName("John")
//                    .lastName("Doe")
//                    .email("john.doe@example.com")
//                    .departmentId(1L)
//                    .build();
//
//            when(employeeService.updateEmployee(eq(999L), any(EmployeeDTO.class)))
//                    .thenThrow(new EmployeeNotFoundException("999"));
//
//            // When & Then
//            mockMvc.perform(put("/api/v1/employees/999")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateRequest)))
//                    .andExpect(status().isNotFound());
//
//            verify(employeeService).updateEmployee(eq(999L), any(EmployeeDTO.class));
//        }
//
//        @Test
//        @DisplayName("Should return 409 when updating to duplicate email")
//        void shouldReturn409WhenUpdatingToDuplicateEmail() throws Exception {
//            // Given
//            EmployeeDTO updateRequest = EmployeeDTO.builder()
//                    .firstName("John")
//                    .lastName("Doe")
//                    .email("existing@example.com")
//                    .departmentId(1L)
//                    .build();
//
//            when(employeeService.updateEmployee(eq(1L), any(EmployeeDTO.class)))
//                    .thenThrow(new DuplicateEmployeeException("email", "existing@example.com"));
//
//            // When & Then
//            mockMvc.perform(put("/api/v1/employees/1")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateRequest)))
//                    .andExpect(status().isConflict());
//
//            verify(employeeService).updateEmployee(eq(1L), any(EmployeeDTO.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("PATCH /api/v1/employees/{id} - Partial Update Employee")
//    class PatchEmployeeTests {
//
//        @Test
//        @DisplayName("Should partially update employee successfully")
//        void shouldPartiallyUpdateEmployeeSuccessfully() throws Exception {
//            // Given
//            EmployeePatchDTO patchRequest = EmployeePatchDTO.builder()
//                    .firstName("John Updated")
//                    .email("john.updated@example.com")
//                    // lastName and departmentId not included - should remain unchanged
//                    .build();
//
//            EmployeeDTO patchedEmployee = EmployeeDTO.builder()
//                    .id(1L)
//                    .firstName("John Updated") // Updated
//                    .lastName("Doe") // Unchanged
//                    .email("john.updated@example.com") // Updated
//                    .departmentId(1L) // Unchanged
//                    .build();
//
//            when(employeeService.patchEmployee(eq(1L), any(EmployeePatchDTO.class))).thenReturn(patchedEmployee);
//
//            // When & Then
//            mockMvc.perform(patch("/api/v1/employees/1")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(patchRequest)))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id", is(1)))
//                    .andExpect(jsonPath("$.firstName", is("John Updated")))
//                    .andExpect(jsonPath("$.lastName", is("Doe")))
//                    .andExpect(jsonPath("$.email", is("john.updated@example.com")))
//                    .andExpect(jsonPath("$.departmentId", is(1)));
//
//            verify(employeeService).patchEmployee(eq(1L), any(EmployeePatchDTO.class));
//        }
//
//        @Test
//        @DisplayName("Should return 404 when patching non-existent employee")
//        void shouldReturn404WhenPatchingNonExistentEmployee() throws Exception {
//            // Given
//            EmployeePatchDTO patchRequest = EmployeePatchDTO.builder()
//                    .firstName("John Updated")
//                    .build();
//
//            when(employeeService.patchEmployee(eq(999L), any(EmployeePatchDTO.class)))
//                    .thenThrow(new EmployeeNotFoundException("999"));
//
//            // When & Then
//            mockMvc.perform(patch("/api/v1/employees/999")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(patchRequest)))
//                    .andExpect(status().isNotFound());
//
//            verify(employeeService).patchEmployee(eq(999L), any(EmployeePatchDTO.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("DELETE /api/v1/employees/{id} - Delete Employee")
//    class DeleteEmployeeTests {
//
//        @Test
//        @DisplayName("Should delete employee successfully")
//        void shouldDeleteEmployeeSuccessfully() throws Exception {
//            // Given
//            doNothing().when(employeeService).deleteEmployee(1L);
//
//            // When & Then
//            mockMvc.perform(delete("/api/v1/employees/1"))
//                    .andDo(print())
//                    .andExpect(status().isNoContent())
//                    .andExpect(content().string(""));
//
//            verify(employeeService).deleteEmployee(1L);
//        }
//
//        @Test
//        @DisplayName("Should return 404 when deleting non-existent employee")
//        void shouldReturn404WhenDeletingNonExistentEmployee() throws Exception {
//            // Given
//            doThrow(new EmployeeNotFoundException("999")).when(employeeService).deleteEmployee(999L);
//
//            // When & Then
//            mockMvc.perform(delete("/api/v1/employees/999"))
//                    .andExpect(status().isNotFound());
//
//            verify(employeeService).deleteEmployee(999L);
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/employees/search - Search Employees")
//    class SearchEmployeesTests {
//
//        @Test
//        @DisplayName("Should return search results")
//        void shouldReturnSearchResults() throws Exception {
//            // Given
//            List<EmployeeDTO> searchResults = Arrays.asList(sampleEmployee, sampleEmployee2);
//            when(employeeService.searchEmployees("John")).thenReturn(searchResults);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/search")
//                            .param("q", "John"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$", hasSize(2)))
//                    .andExpect(jsonPath("$[0].firstName", is("John")))
//                    .andExpect(jsonPath("$[1].firstName", is("Jane")));
//
//            verify(employeeService).searchEmployees("John");
//        }
//
//        @Test
//        @DisplayName("Should return empty results when no matches found")
//        void shouldReturnEmptyResultsWhenNoMatches() throws Exception {
//            // Given
//            when(employeeService.searchEmployees("NonExistent")).thenReturn(Arrays.asList());
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/search")
//                            .param("q", "NonExistent"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$", hasSize(0)));
//
//            verify(employeeService).searchEmployees("NonExistent");
//        }
//
//        @Test
//        @DisplayName("Should handle search with special characters")
//        void shouldHandleSearchWithSpecialCharacters() throws Exception {
//            // Given
//            when(employeeService.searchEmployees("john@example.com")).thenReturn(Arrays.asList(sampleEmployee));
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/search")
//                            .param("q", "john@example.com"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$", hasSize(1)));
//
//            verify(employeeService).searchEmployees("john@example.com");
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/employees/stats - Get Employee Statistics")
//    class GetEmployeeStatsTests {
//
//        @Test
//        @DisplayName("Should return employee statistics")
//        void shouldReturnEmployeeStatistics() throws Exception {
//            // Given
//            Map<Long, Long> employeesByDept = new HashMap<>();
//            employeesByDept.put(1L, 5L);
//            employeesByDept.put(2L, 3L);
//
//            Map<String, Long> employeesByDeptName = new HashMap<>();
//            employeesByDeptName.put("Engineering", 5L);
//            employeesByDeptName.put("Marketing", 3L);
//
//            EmployeeStatsDTO stats = new EmployeeStatsDTO();
//            stats.setTotalEmployees(8L);
//            stats.setEmployeesByDepartment(employeesByDept);
//            stats.setEmployeesByDepartmentName(employeesByDeptName);
//            stats.setDepartmentsWithEmployees(2L);
//            stats.setAverageEmployeesPerDepartment(4.0);
//
//            when(employeeService.getEmployeeStats()).thenReturn(stats);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/stats"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.totalEmployees", is(8)))
//                    .andExpect(jsonPath("$.departmentsWithEmployees", is(2)))
//                    .andExpect(jsonPath("$.averageEmployeesPerDepartment", is(4.0)))
//                    .andExpect(jsonPath("$.employeesByDepartment['1']", is(5)))
//                    .andExpect(jsonPath("$.employeesByDepartment['2']", is(3)))
//                    .andExpect(jsonPath("$.employeesByDepartmentName['Engineering']", is(5)))
//                    .andExpect(jsonPath("$.employeesByDepartmentName['Marketing']", is(3)));
//
//            verify(employeeService).getEmployeeStats();
//        }
//
//        @Test
//        @DisplayName("Should return empty stats when no employees")
//        void shouldReturnEmptyStatsWhenNoEmployees() throws Exception {
//            // Given
//            EmployeeStatsDTO emptyStats = new EmployeeStatsDTO();
//            emptyStats.setTotalEmployees(0L);
//            emptyStats.setEmployeesByDepartment(new HashMap<>());
//            emptyStats.setEmployeesByDepartmentName(new HashMap<>());
//            emptyStats.setDepartmentsWithEmployees(0L);
//            emptyStats.setAverageEmployeesPerDepartment(0.0);
//
//            when(employeeService.getEmployeeStats()).thenReturn(emptyStats);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/stats"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.totalEmployees", is(0)))
//                    .andExpect(jsonPath("$.departmentsWithEmployees", is(0)))
//                    .andExpect(jsonPath("$.averageEmployeesPerDepartment", is(0.0)));
//
//            verify(employeeService).getEmployeeStats();
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/employees/count - Count Employees By Department")
//    class CountEmployeesByDepartmentTests {
//
//        @Test
//        @DisplayName("Should return employee count for department")
//        void shouldReturnEmployeeCountForDepartment() throws Exception {
//            // Given
//            when(employeeService.countByDepartmentId(1L)).thenReturn(5L);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/count")
//                            .param("departmentId", "1"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(content().string("5"));
//
//            verify(employeeService).countByDepartmentId(1L);
//        }
//
//        @Test
//        @DisplayName("Should return zero count for department with no employees")
//        void shouldReturnZeroCountForEmptyDepartment() throws Exception {
//            // Given
//            when(employeeService.countByDepartmentId(999L)).thenReturn(0L);
//
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/count")
//                            .param("departmentId", "999"))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string("0"));
//
//            verify(employeeService).countByDepartmentId(999L);
//        }
//
//        @Test
//        @DisplayName("Should return 400 when departmentId parameter is missing")
//        void shouldReturn400WhenDepartmentIdMissing() throws Exception {
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/count"))
//                    .andExpect(status().isBadRequest());
//
//            verify(employeeService, never()).countByDepartmentId(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("Error Handling Tests")
//    class ErrorHandlingTests {
//
//        @Test
//        @DisplayName("Should handle malformed JSON in request body")
//        void shouldHandleMalformedJsonInRequestBody() throws Exception {
//            // Given
//            String malformedJson = "{ \"firstName\": \"John\", \"lastName\": }"; // Missing value
//
//            // When & Then
//            mockMvc.perform(post("/api/v1/employees")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(malformedJson))
//                    .andExpect(status().isBadRequest());
//
//            verify(employeeService, never()).create(any(), any());
//        }
//
//        @Test
//        @DisplayName("Should handle missing required request body")
//        void shouldHandleMissingRequestBody() throws Exception {
//            // When & Then
//            mockMvc.perform(post("/api/v1/employees")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isBadRequest());
//
//            verify(employeeService, never()).create(any(), any());
//        }
//
//        @Test
//        @DisplayName("Should handle unsupported HTTP method")
//        void shouldHandleUnsupportedHttpMethod() throws Exception {
//            // When & Then
//            mockMvc.perform(put("/api/v1/employees/search")) // PUT not supported for search
//                    .andExpect(status().isMethodNotAllowed());
//        }
//
//        @Test
//        @DisplayName("Should handle invalid path variable format")
//        void shouldHandleInvalidPathVariableFormat() throws Exception {
//            // When & Then
//            mockMvc.perform(get("/api/v1/employees/invalid-id")) // Should be Long
//                    .andExpect(status().isBadRequest());
//
//            verify(employeeService, never()).getById(any(), anyBoolean());
//        }
//    }
//}