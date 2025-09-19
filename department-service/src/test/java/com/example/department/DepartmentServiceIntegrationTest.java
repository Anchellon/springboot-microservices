package com.example.department;

import com.example.department.client.EmployeeClient;
import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import com.example.department.dto.EmployeeDTO;
import com.example.department.repo.DepartmentRepository;
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
@DisplayName("Department Service Integration Tests (No DB)")
class DepartmentServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentRepository departmentRepository;

    @MockBean
    private EmployeeClient employeeClient;

    private String baseUrl;
    private Department sampleDepartment1;
    private Department sampleDepartment2;
    private DepartmentDTO sampleDepartmentDTO;
    private EmployeeDTO sampleEmployee;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/departments";
        Mockito.reset(departmentRepository, employeeClient);

        sampleDepartment1 = Department.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Builds and maintains products")
                .build();

        sampleDepartment2 = Department.builder()
                .id(2L)
                .name("HR")
                .code("HR")
                .description("People operations and recruiting")
                .build();

        sampleDepartmentDTO = DepartmentDTO.builder()
                .name("Marketing")
                .code("MKT")
                .description("Marketing and communications")
                .build();

        sampleEmployee = EmployeeDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();
    }

    // ========================================
    // GET ALL DEPARTMENTS TESTS
    // ========================================

    @Test
    @DisplayName("GET /departments - should return paginated departments")
    void getAllDepartments_ShouldReturnPaginatedResults() {
        // Arrange
        List<Department> departments = List.of(sampleDepartment1, sampleDepartment2);
        Page<Department> departmentPage = new PageImpl<>(departments, PageRequest.of(0, 10), 2);

        when(departmentRepository.findWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(departmentPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/?page=0&size=10",
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
        assertThat(response.getBody().get("totalPages")).isEqualTo(1);
        assertThat(response.getBody().get("size")).isEqualTo(10);
        assertThat(response.getBody().get("number")).isEqualTo(0);

        Map<String, Object> firstDepartment = content.get(0);
        assertThat(firstDepartment.get("id")).isEqualTo(1);
        assertThat(firstDepartment.get("name")).isEqualTo("Engineering");
        assertThat(firstDepartment.get("code")).isEqualTo("ENG");

        Map<String, Object> secondDepartment = content.get(1);
        assertThat(secondDepartment.get("id")).isEqualTo(2);
        assertThat(secondDepartment.get("name")).isEqualTo("HR");
        assertThat(secondDepartment.get("code")).isEqualTo("HR");
    }

    @Test
    @DisplayName("GET /departments with filters - should filter by name")
    void getAllDepartments_WithNameFilter_ShouldReturnFilteredResults() {
        // Arrange
        List<Department> filteredDepartments = List.of(sampleDepartment1);
        Page<Department> departmentPage = new PageImpl<>(filteredDepartments, PageRequest.of(0, 10), 1);

        when(departmentRepository.findWithFilters(eq("Eng"), isNull(), any(Pageable.class)))
                .thenReturn(departmentPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/?page=0&size=10&nameContains=Eng",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("name")).isEqualTo("Engineering");
        assertThat(response.getBody().get("totalElements")).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /departments with sorting - should return sorted results")
    void getAllDepartments_WithSorting_ShouldReturnSortedResults() {
        // Arrange
        List<Department> sortedDepartments = List.of(sampleDepartment1, sampleDepartment2);
        Page<Department> departmentPage = new PageImpl<>(sortedDepartments,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")), 2);

        when(departmentRepository.findWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(departmentPage);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/?page=0&size=10&sort=name,asc",
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

    // ========================================
    // GET DEPARTMENT BY ID TESTS
    // ========================================

    @Test
    @DisplayName("GET /departments/{id} - should return department when found")
    void getDepartmentById_WhenExists_ShouldReturnDepartment() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));

        // Act
        ResponseEntity<DepartmentDTO> response = restTemplate.getForEntity(
                baseUrl + "/1",
                DepartmentDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getName()).isEqualTo("Engineering");
        assertThat(response.getBody().getCode()).isEqualTo("ENG");
        assertThat(response.getBody().getDescription()).isEqualTo("Builds and maintains products");
    }

    @Test
    @DisplayName("GET /departments/{id} - should return 404 when not found")
    void getDepartmentById_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title")).isEqualTo("Department Not Found");
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("detail").toString()).contains("Department not found with id: 999");
    }

    // ========================================
    // GET DEPARTMENT BY CODE TESTS
    // ========================================

    @Test
    @DisplayName("GET /departments/by-code/{code} - should return department when found")
    void getDepartmentByCode_WhenExists_ShouldReturnDepartment() {
        // Arrange
        when(departmentRepository.findByCode("ENG")).thenReturn(Optional.of(sampleDepartment1));

        // Act
        ResponseEntity<DepartmentDTO> response = restTemplate.getForEntity(
                baseUrl + "/by-code/ENG",
                DepartmentDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getName()).isEqualTo("Engineering");
        assertThat(response.getBody().getCode()).isEqualTo("ENG");
    }

    @Test
    @DisplayName("GET /departments/by-code/{code} - should return 404 when not found")
    void getDepartmentByCode_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(departmentRepository.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/by-code/NONEXISTENT",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title")).isEqualTo("Department Not Found");
    }

    // ========================================
    // CREATE DEPARTMENT TESTS
    // ========================================

    @Test
    @DisplayName("POST /departments - should create department successfully")
    void createDepartment_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        Department savedDepartment = Department.builder()
                .id(3L)
                .name("Marketing")
                .code("MKT")
                .description("Marketing and communications")
                .build();

        when(departmentRepository.existsByName("Marketing")).thenReturn(false);
        when(departmentRepository.existsByCode("MKT")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DepartmentDTO> request = new HttpEntity<>(sampleDepartmentDTO, headers);

        // Act
        ResponseEntity<DepartmentDTO> response = restTemplate.postForEntity(
                baseUrl + "/",
                request,
                DepartmentDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3L);
        assertThat(response.getBody().getName()).isEqualTo("Marketing");
        assertThat(response.getBody().getCode()).isEqualTo("MKT");
        assertThat(response.getBody().getDescription()).isEqualTo("Marketing and communications");
    }

    @Test
    @DisplayName("POST /departments - should return 409 when name already exists")
    void createDepartment_WithDuplicateName_ShouldReturn409() {
        // Arrange
        when(departmentRepository.existsByName("Marketing")).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DepartmentDTO> request = new HttpEntity<>(sampleDepartmentDTO, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Duplicate Department");
        assertThat(response.getBody().get("status")).isEqualTo(409);
    }

    @Test
    @DisplayName("POST /departments - should return 400 for invalid data")
    void createDepartment_WithInvalidData_ShouldReturn400() {
        // Arrange
        DepartmentDTO invalidDto = DepartmentDTO.builder()
                .name("") // Empty name
                .code("TOOLONGCODE") // Invalid code
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DepartmentDTO> request = new HttpEntity<>(invalidDto, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("title")).isEqualTo("Validation Failed");
        @SuppressWarnings("unchecked")
        List<?> errors = (List<?>) response.getBody().get("errors");
        assertThat(errors).isNotEmpty();
    }

    // ========================================
    // UPDATE DEPARTMENT TESTS
    // ========================================

    @Test
    @DisplayName("PUT /departments/{id} - should update department successfully")
    void updateDepartment_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        Department updatedDepartment = Department.builder()
                .id(1L)
                .name("Updated Engineering")
                .code("UENG")
                .description("Updated description")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));
        when(departmentRepository.existsByNameAndIdNot("Updated Engineering", 1L)).thenReturn(false);
        when(departmentRepository.existsByCodeAndIdNot("UENG", 1L)).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        DepartmentDTO updateDto = DepartmentDTO.builder()
                .name("Updated Engineering")
                .code("UENG")
                .description("Updated description")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DepartmentDTO> request = new HttpEntity<>(updateDto, headers);

        // Act
        ResponseEntity<DepartmentDTO> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PUT,
                request,
                DepartmentDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getName()).isEqualTo("Updated Engineering");
        assertThat(response.getBody().getCode()).isEqualTo("UENG");
        assertThat(response.getBody().getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("PUT /departments/{id} - should return 404 when department not found")
    void updateDepartment_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DepartmentDTO> request = new HttpEntity<>(sampleDepartmentDTO, headers);

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

    // ========================================
    // PATCH DEPARTMENT TESTS
    // ========================================

    @Test
    @DisplayName("PATCH /departments/{id} - should partially update department")
    void patchDepartment_WithPartialData_ShouldUpdateSuccessfully() {
        // Arrange
        Department updatedDepartment = Department.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Updated description only")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        DepartmentPatchDTO patchDto = DepartmentPatchDTO.builder()
                .description("Updated description only")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DepartmentPatchDTO> request = new HttpEntity<>(patchDto, headers);

        // Act
        ResponseEntity<DepartmentDTO> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PATCH,
                request,
                DepartmentDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getName()).isEqualTo("Engineering"); // Unchanged
        assertThat(response.getBody().getDescription()).isEqualTo("Updated description only"); // Changed
    }

    // ========================================
    // DELETE DEPARTMENT TESTS
    // ========================================

    @Test
    @DisplayName("DELETE /departments/{id} - should delete department when no employees")
    void deleteDepartment_WithNoEmployees_ShouldDeleteSuccessfully() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));
        when(employeeClient.countByDepartmentId(1L)).thenReturn(0L);

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
    @DisplayName("DELETE /departments/{id} - should return 409 when department has employees")
    void deleteDepartment_WithEmployees_ShouldReturn409() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));
        when(employeeClient.countByDepartmentId(1L)).thenReturn(5L);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Department Cannot Be Deleted");
        assertThat(response.getBody().get("detail").toString()).contains("5 employees are still assigned");
    }

    @Test
    @DisplayName("DELETE /departments/{id} - should return 404 when department not found")
    void deleteDepartment_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

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
    // GET DEPARTMENT WITH EMPLOYEES TESTS
    // ========================================

    @Test
    @DisplayName("GET /departments/{id}/employees - should return department with employees")
    void getDepartmentWithEmployees_ShouldReturnDepartmentAndEmployees() {
        // Arrange
        List<EmployeeDTO> employees = List.of(sampleEmployee);
        Page<EmployeeDTO> employeePage = new PageImpl<>(employees, PageRequest.of(0, 20), 1);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));
        when(employeeClient.getEmployeesByDepartment(eq(1L), eq(0), eq(20), isNull()))
                .thenReturn(employeePage);

        // Act
        ResponseEntity<DepartmentEmployeesDTO> response = restTemplate.exchange(
                baseUrl + "/1/employees?page=0&size=20",
                HttpMethod.GET,
                null,
                DepartmentEmployeesDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDepartment().getId()).isEqualTo(1L);
        assertThat(response.getBody().getDepartment().getName()).isEqualTo("Engineering");
        assertThat(response.getBody().getEmployees().getContent()).hasSize(1);
        assertThat(response.getBody().getEmployees().getContent().get(0).getFirstName()).isEqualTo("John");
        assertThat(response.getBody().getTotalEmployees()).isEqualTo(1L);
        assertThat(response.getBody().getSummary()).contains("Engineering");
    }

    @Test
    @DisplayName("GET /departments/{id}/employees - should return 404 when department not found")
    void getDepartmentWithEmployees_WhenDepartmentNotExists_ShouldReturn404() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999/employees",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @DisplayName("Should handle EmployeeClient failures gracefully")
    void whenEmployeeClientFails_ShouldReturn500() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment1));
        when(employeeClient.countByDepartmentId(1L))
                .thenThrow(new RuntimeException("Employee service unavailable"));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("title")).isEqualTo("Internal Server Error");
    }
}