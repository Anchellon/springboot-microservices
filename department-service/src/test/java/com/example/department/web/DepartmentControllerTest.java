package com.example.department.web;

import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import com.example.department.dto.EmployeeDTO;
import com.example.department.exception.DepartmentInUseException;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.service.DepartmentService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@DisplayName("Department Controller Tests")
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @Autowired
    private ObjectMapper objectMapper;

    private DepartmentDTO sampleDepartment;
    private DepartmentDTO engineeringDept;
    private DepartmentDTO hrDept;
    private List<DepartmentDTO> departmentList;
    private Page<DepartmentDTO> departmentPage;

    @BeforeEach
    void setUp() {
        sampleDepartment = DepartmentDTO.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Software development team")
                .build();

        engineeringDept = DepartmentDTO.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Software development team")
                .build();

        hrDept = DepartmentDTO.builder()
                .id(2L)
                .name("Human Resources")
                .code("HR")
                .description("People operations")
                .build();

        departmentList = Arrays.asList(engineeringDept, hrDept);
        departmentPage = new PageImpl<>(departmentList, PageRequest.of(0, 20), departmentList.size());
    }

    @Nested
    @DisplayName("GET /api/v1/departments - Get All Departments")
    class GetAllDepartmentsTests {

        @Test
        @DisplayName("Should return paginated departments with default parameters")
        void shouldReturnPaginatedDepartmentsWithDefaults() throws Exception {
            // Given
            when(departmentService.findAll(0, 20, null, null, null))
                    .thenReturn(departmentPage);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].name", is("Engineering")))
                    .andExpect(jsonPath("$.content[0].code", is("ENG")))
                    .andExpect(jsonPath("$.content[1].id", is(2)))
                    .andExpect(jsonPath("$.content[1].name", is("Human Resources")))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.size", is(20)))
                    .andExpect(jsonPath("$.number", is(0)));

            verify(departmentService).findAll(0, 20, null, null, null);
        }

        @Test
        @DisplayName("Should return departments with custom pagination parameters")
        void shouldReturnDepartmentsWithCustomPagination() throws Exception {
            // Given
            Page<DepartmentDTO> customPage = new PageImpl<>(
                    Collections.singletonList(engineeringDept),
                    PageRequest.of(1, 5),
                    10
            );
            when(departmentService.findAll(1, 5, "name,desc", null, null))
                    .thenReturn(customPage);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "name,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(10)))
                    .andExpect(jsonPath("$.size", is(5)))
                    .andExpect(jsonPath("$.number", is(1)));

            verify(departmentService).findAll(1, 5, "name,desc", null, null);
        }

        @Test
        @DisplayName("Should return filtered departments by name")
        void shouldReturnFilteredDepartmentsByName() throws Exception {
            // Given
            Page<DepartmentDTO> filteredPage = new PageImpl<>(
                    Collections.singletonList(engineeringDept),
                    PageRequest.of(0, 20),
                    1
            );
            when(departmentService.findAll(0, 20, null, "Eng", null))
                    .thenReturn(filteredPage);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/")
                            .param("nameContains", "Eng"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", is("Engineering")))
                    .andExpect(jsonPath("$.totalElements", is(1)));

            verify(departmentService).findAll(0, 20, null, "Eng", null);
        }

        @Test
        @DisplayName("Should return filtered departments by code")
        void shouldReturnFilteredDepartmentsByCode() throws Exception {
            // Given
            Page<DepartmentDTO> filteredPage = new PageImpl<>(
                    Collections.singletonList(hrDept),
                    PageRequest.of(0, 20),
                    1
            );
            when(departmentService.findAll(0, 20, null, null, "HR"))
                    .thenReturn(filteredPage);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/")
                            .param("codeContains", "HR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].code", is("HR")))
                    .andExpect(jsonPath("$.totalElements", is(1)));

            verify(departmentService).findAll(0, 20, null, null, "HR");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/departments/{id} - Get Department by ID")
    class GetDepartmentByIdTests {

        @Test
        @DisplayName("Should return department when exists")
        void shouldReturnDepartmentWhenExists() throws Exception {
            // Given
            when(departmentService.findById(1L)).thenReturn(sampleDepartment);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/{id}", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Engineering")))
                    .andExpect(jsonPath("$.code", is("ENG")))
                    .andExpect(jsonPath("$.description", is("Software development team")));

            verify(departmentService).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 when department not found")
        void shouldReturn404WhenDepartmentNotFound() throws Exception {
            // Given
            when(departmentService.findById(999L))
                    .thenThrow(new DepartmentNotFoundException("Department not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/departments/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(departmentService).findById(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/departments/by-code/{code} - Get Department by Code")
    class GetDepartmentByCodeTests {

        @Test
        @DisplayName("Should return department when code exists")
        void shouldReturnDepartmentWhenCodeExists() throws Exception {
            // Given
            when(departmentService.findByCode("ENG")).thenReturn(sampleDepartment);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/by-code/{code}", "ENG"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Engineering")))
                    .andExpect(jsonPath("$.code", is("ENG")));

            verify(departmentService).findByCode("ENG");
        }

        @Test
        @DisplayName("Should return 404 when code not found")
        void shouldReturn404WhenCodeNotFound() throws Exception {
            // Given
            when(departmentService.findByCode("INVALID"))
                    .thenThrow(new DepartmentNotFoundException("Department not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/departments/by-code/{code}", "INVALID"))
                    .andExpect(status().isNotFound());

            verify(departmentService).findByCode("INVALID");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/departments - Create Department")
    class CreateDepartmentTests {

        @Test
        @DisplayName("Should create department with valid data")
        void shouldCreateDepartmentWithValidData() throws Exception {
            // Given
            DepartmentDTO createRequest = DepartmentDTO.builder()
                    .name("Marketing")
                    .code("MKT")
                    .description("Marketing and sales")
                    .build();

            DepartmentDTO createdDepartment = DepartmentDTO.builder()
                    .id(3L)
                    .name("Marketing")
                    .code("MKT")
                    .description("Marketing and sales")
                    .build();

            when(departmentService.create(any(DepartmentDTO.class))).thenReturn(createdDepartment);

            // When & Then
            mockMvc.perform(post("/api/v1/departments/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.name", is("Marketing")))
                    .andExpect(jsonPath("$.code", is("MKT")))
                    .andExpect(jsonPath("$.description", is("Marketing and sales")));

            verify(departmentService).create(any(DepartmentDTO.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid department data")
        void shouldReturn400ForInvalidDepartmentData() throws Exception {
            // Given - Invalid department (missing required fields)
            DepartmentDTO invalidRequest = DepartmentDTO.builder()
                    .description("Only description")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/departments/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid code pattern")
        void shouldReturn400ForInvalidCodePattern() throws Exception {
            // Given - Invalid code pattern
            DepartmentDTO invalidRequest = DepartmentDTO.builder()
                    .name("Marketing")
                    .code("mk") // lowercase, should be uppercase
                    .description("Marketing team")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/departments/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/departments/{id} - Update Department")
    class UpdateDepartmentTests {

        @Test
        @DisplayName("Should update department with valid data")
        void shouldUpdateDepartmentWithValidData() throws Exception {
            // Given
            DepartmentDTO updateRequest = DepartmentDTO.builder()
                    .name("Updated Engineering")
                    .code("UEG")
                    .description("Updated description")
                    .build();

            DepartmentDTO updatedDepartment = DepartmentDTO.builder()
                    .id(1L)
                    .name("Updated Engineering")
                    .code("UEG")
                    .description("Updated description")
                    .build();

            when(departmentService.updateDepartment(eq(1L), any(DepartmentDTO.class)))
                    .thenReturn(updatedDepartment);

            // When & Then
            mockMvc.perform(put("/api/v1/departments/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Updated Engineering")))
                    .andExpect(jsonPath("$.code", is("UEG")))
                    .andExpect(jsonPath("$.description", is("Updated description")));

            verify(departmentService).updateDepartment(eq(1L), any(DepartmentDTO.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent department")
        void shouldReturn404WhenUpdatingNonExistentDepartment() throws Exception {
            // Given
            DepartmentDTO updateRequest = DepartmentDTO.builder()
                    .name("Updated Name")
                    .code("UPD")
                    .build();

            when(departmentService.updateDepartment(eq(999L), any(DepartmentDTO.class)))
                    .thenThrow(new DepartmentNotFoundException("Department not found"));

            // When & Then
            mockMvc.perform(put("/api/v1/departments/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());

            verify(departmentService).updateDepartment(eq(999L), any(DepartmentDTO.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/departments/{id} - Partial Update Department")
    class PatchDepartmentTests {

        @Test
        @DisplayName("Should partially update department")
        void shouldPartiallyUpdateDepartment() throws Exception {
            // Given
            DepartmentPatchDTO patchRequest = DepartmentPatchDTO.builder()
                    .name("Patched Engineering")
                    .build();

            DepartmentDTO patchedDepartment = DepartmentDTO.builder()
                    .id(1L)
                    .name("Patched Engineering")
                    .code("ENG") // unchanged
                    .description("Software development team") // unchanged
                    .build();

            when(departmentService.patchDepartment(eq(1L), any(DepartmentPatchDTO.class)))
                    .thenReturn(patchedDepartment);

            // When & Then
            mockMvc.perform(patch("/api/v1/departments/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Patched Engineering")))
                    .andExpect(jsonPath("$.code", is("ENG")))
                    .andExpect(jsonPath("$.description", is("Software development team")));

            verify(departmentService).patchDepartment(eq(1L), any(DepartmentPatchDTO.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid patch data")
        void shouldReturn400ForInvalidPatchData() throws Exception {
            // Given - Invalid code pattern in patch
            DepartmentPatchDTO invalidPatch = DepartmentPatchDTO.builder()
                    .code("invalid") // lowercase
                    .build();

            // When & Then
            mockMvc.perform(patch("/api/v1/departments/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidPatch)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/departments/{id} - Delete Department")
    class DeleteDepartmentTests {

        @Test
        @DisplayName("Should delete department successfully")
        void shouldDeleteDepartmentSuccessfully() throws Exception {
            // Given - Service method doesn't throw exception (successful deletion)

            // When & Then
            mockMvc.perform(delete("/api/v1/departments/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(departmentService).deleteDepartment(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent department")
        void shouldReturn404WhenDeletingNonExistentDepartment() throws Exception {
            // Given
            doThrow(new DepartmentNotFoundException("Department not found"))
                    .when(departmentService).deleteDepartment(999L);

            // When & Then
            mockMvc.perform(delete("/api/v1/departments/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(departmentService).deleteDepartment(999L);
        }

        @Test
        @DisplayName("Should return 409 when deleting department with employees")
        void shouldReturn409WhenDeletingDepartmentWithEmployees() throws Exception {
            // Given - Use doThrow for void methods
            doThrow(new DepartmentInUseException(1L, "Department has employees", 5L))
                    .when(departmentService).deleteDepartment(1L);

            // When & Then
            mockMvc.perform(delete("/api/v1/departments/{id}", 1L))
                    .andExpect(status().isConflict());

            verify(departmentService).deleteDepartment(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/departments/{id}/employees - Get Department with Employees")
    class GetDepartmentWithEmployeesTests {

        @Test
        @DisplayName("Should return department with paginated employees")
        void shouldReturnDepartmentWithPaginatedEmployees() throws Exception {
            // Given
            List<EmployeeDTO> employees = Arrays.asList(
                    EmployeeDTO.builder().id(1L).firstName("John").lastName("Doe")
                            .email("john.doe@example.com").departmentId(1L).build(),
                    EmployeeDTO.builder().id(2L).firstName("Jane").lastName("Smith")
                            .email("jane.smith@example.com").departmentId(1L).build()
            );

            Page<EmployeeDTO> employeePage = new PageImpl<>(employees, PageRequest.of(0, 20), employees.size());

            DepartmentEmployeesDTO departmentWithEmployees = DepartmentEmployeesDTO.builder()
                    .department(sampleDepartment)
                    .employees(employeePage)
                    .totalEmployees(employees.size())
                    .summary("Engineering department has 2 employees")
                    .build();

            when(departmentService.getDepartmentWithEmployees(1L, 0, 20, null))
                    .thenReturn(departmentWithEmployees);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/{id}/employees", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.department.id", is(1)))
                    .andExpect(jsonPath("$.department.name", is("Engineering")))
                    .andExpect(jsonPath("$.employees.content", hasSize(2)))
                    .andExpect(jsonPath("$.employees.content[0].firstName", is("John")))
                    .andExpect(jsonPath("$.employees.content[1].firstName", is("Jane")))
                    .andExpect(jsonPath("$.totalEmployees", is(2)))
                    .andExpect(jsonPath("$.summary", containsString("Engineering department has 2 employees")));

            verify(departmentService).getDepartmentWithEmployees(1L, 0, 20, null);
        }

        @Test
        @DisplayName("Should return department with employees using custom pagination")
        void shouldReturnDepartmentWithEmployeesUsingCustomPagination() throws Exception {
            // Given
            List<EmployeeDTO> employees = Collections.singletonList(
                    EmployeeDTO.builder().id(1L).firstName("John").lastName("Doe")
                            .email("john.doe@example.com").departmentId(1L).build()
            );

            Page<EmployeeDTO> employeePage = new PageImpl<>(employees, PageRequest.of(0, 5), 10);

            DepartmentEmployeesDTO departmentWithEmployees = DepartmentEmployeesDTO.builder()
                    .department(sampleDepartment)
                    .employees(employeePage)
                    .totalEmployees(10)
                    .summary("Engineering department has 10 employees")
                    .build();

            when(departmentService.getDepartmentWithEmployees(1L, 0, 5, "lastName,asc"))
                    .thenReturn(departmentWithEmployees);

            // When & Then
            mockMvc.perform(get("/api/v1/departments/{id}/employees", 1L)
                            .param("page", "0")
                            .param("size", "5")
                            .param("sort", "lastName,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employees.content", hasSize(1)))
                    .andExpect(jsonPath("$.employees.totalElements", is(10)))
                    .andExpect(jsonPath("$.employees.size", is(5)))
                    .andExpect(jsonPath("$.totalEmployees", is(10)));

            verify(departmentService).getDepartmentWithEmployees(1L, 0, 5, "lastName,asc");
        }

        @Test
        @DisplayName("Should return 404 when department not found for employees")
        void shouldReturn404WhenDepartmentNotFoundForEmployees() throws Exception {
            // Given
            when(departmentService.getDepartmentWithEmployees(999L, 0, 20, null))
                    .thenThrow(new DepartmentNotFoundException("Department not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/departments/{id}/employees", 999L))
                    .andExpect(status().isNotFound());

            verify(departmentService).getDepartmentWithEmployees(999L, 0, 20, null);
        }
    }
}