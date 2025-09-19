package com.example.employee;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeePatchDTO;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.service.IdempotencyService;
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
@DisplayName("Employee Controller Integration Tests (No DB)")
class EmployeeServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private DepartmentClient departmentClient;

    @MockBean
    private IdempotencyService idempotencyService;

    private String baseUrl;
    private Employee sampleEmployee1;
    private Employee sampleEmployee2;
    private Employee sampleEmployee3;
    private EmployeeDTO sampleEmployeeDTO;
    private DepartmentDTO sampleDepartment1;
    private DepartmentDTO sampleDepartment2;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/employees";
        Mockito.reset(employeeRepository, departmentClient, idempotencyService);

        // Sample departments
        sampleDepartment1 = DepartmentDTO.builder()
                .id(1L)
                .name("Engineering")
                .description("Builds and maintains products")
                .build();

        sampleDepartment2 = DepartmentDTO.builder()
                .id(2L)
                .name("HR")
                .description("People operations and recruiting")
                .build();

        // Sample employees
        sampleEmployee1 = Employee.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Nguyen")
                .email("alice@example.com")
                .departmentId(1L)
                .build();

        sampleEmployee2 = Employee.builder()
                .id(2L)
                .firstName("Bob")
                .lastName("Martinez")
                .email("bob@example.com")
                .departmentId(1L)
                .build();

        sampleEmployee3 = Employee.builder()
                .id(3L)
                .firstName("Carla")
                .lastName("Singh")
                .email("carla@example.com")
                .departmentId(2L)
                .build();

        // Sample DTO for creation/updates
        sampleEmployeeDTO = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();
    }

    // ========================================
    // GET ALL EMPLOYEES TESTS (PAGINATED)
    // ========================================

    @Test
    @DisplayName("GET /employees - should return paginated employees with department enrichment")
    void getAllEmployees_ShouldReturnPaginatedResultsWithDepartments() {
        // Arrange
        List<Employee> employees = List.of(sampleEmployee1, sampleEmployee2, sampleEmployee3);
        Page<Employee> employeePage = new PageImpl<>(employees, PageRequest.of(0, 20), 3);

        when(employeeRepository.findWithFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(employeePage);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);
        when(departmentClient.getDepartment(2L)).thenReturn(sampleDepartment2);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/?page=0&size=20",
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

        Map<String, Object> firstEmployee = content.get(0);
        assertThat(firstEmployee.get("id")).isEqualTo(1);
        assertThat(firstEmployee.get("firstName")).isEqualTo("Alice");
        assertThat(firstEmployee.get("lastName")).isEqualTo("Nguyen");
        assertThat(firstEmployee.get("email")).isEqualTo("alice@example.com");
        assertThat(firstEmployee.get("departmentId")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> department = (Map<String, Object>) firstEmployee.get("department");
        assertThat(department.get("name")).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("GET /employees with email filter - should filter by email")
    void getAllEmployees_WithEmailFilter_ShouldReturnFilteredResults() {
        // Arrange
        List<Employee> filteredEmployees = List.of(sampleEmployee1);
        Page<Employee> employeePage = new PageImpl<>(filteredEmployees, PageRequest.of(0, 20), 1);

        when(employeeRepository.findWithFilters(eq("alice@example.com"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(employeePage);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/?email=alice@example.com&page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("email")).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("GET /employees with departmentId filter - should filter by department")
    void getAllEmployees_WithDepartmentFilter_ShouldReturnFilteredResults() {
        // Arrange
        List<Employee> filteredEmployees = List.of(sampleEmployee1, sampleEmployee2);
        Page<Employee> employeePage = new PageImpl<>(filteredEmployees, PageRequest.of(0, 20), 2);

        when(employeeRepository.findWithFilters(isNull(), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(employeePage);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/?departmentId=1&page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).get("departmentId")).isEqualTo(1);
        assertThat(content.get(1).get("departmentId")).isEqualTo(1);
    }

    // ========================================
    // GET EMPLOYEE BY ID TESTS
    // ========================================

    @Test
    @DisplayName("GET /employees/{id} - should return employee when found with department enrichment")
    void getEmployeeById_WhenExists_ShouldReturnEmployeeWithDepartment() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee1));
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
                baseUrl + "/1?enrichWithDepartment=true",
                HttpMethod.GET,
                null,
                EmployeeDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFirstName()).isEqualTo("Alice");
        assertThat(response.getBody().getLastName()).isEqualTo("Nguyen");
        assertThat(response.getBody().getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getBody().getDepartmentId()).isEqualTo(1L);
        assertThat(response.getBody().getDepartment().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("GET /employees/{id} - should return employee without department when enrichWithDepartment=false")
    void getEmployeeById_WithoutDepartmentEnrichment_ShouldReturnEmployeeOnly() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee1));

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
                baseUrl + "/1?enrichWithDepartment=false",
                HttpMethod.GET,
                null,
                EmployeeDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFirstName()).isEqualTo("Alice");
        assertThat(response.getBody().getDepartment()).isNull();
    }

    @Test
    @DisplayName("GET /employees/{id} - should return 404 when not found")
    void getEmployeeById_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

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
    // CREATE EMPLOYEE TESTS
    // ========================================

    @Test
    @DisplayName("POST /employees - should create employee successfully")
    void createEmployee_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        Employee savedEmployee = Employee.builder()
                .id(4L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();

        when(employeeRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);
        when(idempotencyService.isProcessed(anyString())).thenReturn(false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(sampleEmployeeDTO, headers);

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(baseUrl + "/", request, EmployeeDTO.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(4L);
        assertThat(response.getBody().getFirstName()).isEqualTo("John");
        assertThat(response.getBody().getLastName()).isEqualTo("Doe");
        assertThat(response.getBody().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getBody().getDepartmentId()).isEqualTo(1L);
        assertThat(response.getBody().getDepartment().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("POST /employees with idempotency key - should create employee successfully")
    void createEmployee_WithIdempotencyKey_ShouldCreateSuccessfully() {
        // Arrange
        Employee savedEmployee = Employee.builder()
                .id(4L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();

        when(employeeRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);
        when(idempotencyService.isProcessed("key123")).thenReturn(false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "key123");
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(sampleEmployeeDTO, headers);

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(baseUrl + "/", request, EmployeeDTO.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("POST /employees with duplicate idempotency key - should return cached result")
    void createEmployee_WithDuplicateIdempotencyKey_ShouldReturnCachedResult() {
        // Arrange
        EmployeeDTO cachedResult = EmployeeDTO.builder()
                .id(4L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .department(sampleDepartment1)
                .build();

        when(idempotencyService.isProcessed("key123")).thenReturn(true);
        when(idempotencyService.getResult("key123")).thenReturn(cachedResult);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "key123");
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(sampleEmployeeDTO, headers);

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(baseUrl + "/", request, EmployeeDTO.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(4L);
        assertThat(response.getBody().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("POST /employees - should return 409 when email already exists")
    void createEmployee_WithDuplicateEmail_ShouldReturn409() {
        // Arrange
        when(employeeRepository.existsByEmail("john.doe@example.com")).thenReturn(true);
        when(idempotencyService.isProcessed(anyString())).thenReturn(false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(sampleEmployeeDTO, headers);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Business Rule Violation");
        assertThat(response.getBody().get("status")).isEqualTo(409);
    }

    @Test
    @DisplayName("POST /employees - should return 400 for invalid data")
    void createEmployee_WithInvalidData_ShouldReturn400() {
        // Arrange
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName("") // Empty first name
                .lastName("Doe")
                .email("invalid-email") // Invalid email
                .departmentId(1L)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(invalidDto, headers);

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
        assertThat((List<?>) response.getBody().get("errors")).isNotEmpty();
    }

    // ========================================
    // UPDATE EMPLOYEE TESTS
    // ========================================

    @Test
    @DisplayName("PUT /employees/{id} - should update employee successfully")
    void updateEmployee_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        Employee updatedEmployee = Employee.builder()
                .id(1L)
                .firstName("Alice Updated")
                .lastName("Nguyen Updated")
                .email("alice.updated@example.com")
                .departmentId(2L)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee1));
        when(employeeRepository.existsByEmailAndIdNot("alice.updated@example.com", 1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(departmentClient.getDepartment(2L)).thenReturn(sampleDepartment2);

        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Alice Updated")
                .lastName("Nguyen Updated")
                .email("alice.updated@example.com")
                .departmentId(2L)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(updateDto, headers);

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PUT,
                request,
                EmployeeDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFirstName()).isEqualTo("Alice Updated");
        assertThat(response.getBody().getEmail()).isEqualTo("alice.updated@example.com");
        assertThat(response.getBody().getDepartmentId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("PUT /employees/{id} - should return 404 when employee not found")
    void updateEmployee_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(sampleEmployeeDTO, headers);

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
    // PATCH EMPLOYEE TESTS
    // ========================================

    @Test
    @DisplayName("PATCH /employees/{id} - should partially update employee")
    void patchEmployee_WithPartialData_ShouldUpdateSuccessfully() {
        // Arrange
        Employee updatedEmployee = Employee.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Nguyen")
                .email("alice@example.com")
                .departmentId(2L) // Only department changed
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee1));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(departmentClient.getDepartment(2L)).thenReturn(sampleDepartment2);

        EmployeePatchDTO patchDto = EmployeePatchDTO.builder()
                .departmentId(2L) // Only updating department
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeePatchDTO> request = new HttpEntity<>(patchDto, headers);

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
                baseUrl + "/1",
                HttpMethod.PATCH,
                request,
                EmployeeDTO.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFirstName()).isEqualTo("Alice"); // Unchanged
        assertThat(response.getBody().getDepartmentId()).isEqualTo(2L); // Changed
    }

    // ========================================
    // DELETE EMPLOYEE TESTS
    // ========================================

    @Test
    @DisplayName("DELETE /employees/{id} - should delete employee successfully")
    void deleteEmployee_WhenExists_ShouldDeleteSuccessfully() {
        // Arrange
        when(employeeRepository.existsById(1L)).thenReturn(true);

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
    @DisplayName("DELETE /employees/{id} - should return 404 when employee not found")
    void deleteEmployee_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(employeeRepository.existsById(999L)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title")).isEqualTo("Resource Not Found");
    }

    // ========================================
    // SEARCH EMPLOYEES TESTS
    // ========================================

    @Test
    @DisplayName("GET /employees/search - should search employees by query")
    void searchEmployees_WithValidQuery_ShouldReturnMatchingEmployees() {
        // Arrange
        List<Employee> searchResults = List.of(sampleEmployee1);
        when(employeeRepository.searchByNameOrEmail("alice")).thenReturn(searchResults);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);

        // Act
        ResponseEntity<List<EmployeeDTO>> response = restTemplate.exchange(
                baseUrl + "/search?q=alice",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<EmployeeDTO>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getFirstName()).isEqualTo("Alice");
        assertThat(response.getBody().get(0).getEmail()).isEqualTo("alice@example.com");
    }

    // ========================================
    // EMPLOYEE STATISTICS TESTS
    // ========================================

    @Test
    @DisplayName("GET /employees/stats - should return employee statistics")
    void getEmployeeStats_ShouldReturnStatistics() {
        // Arrange
        when(employeeRepository.count()).thenReturn(3L);
        when(employeeRepository.countByDepartment()).thenReturn(List.of(
                new Object[]{1L, 2L}, // Department 1: 2 employees
                new Object[]{2L, 1L}  // Department 2: 1 employee
        ));
        when(employeeRepository.countDistinctDepartments()).thenReturn(2L);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment1);
        when(departmentClient.getDepartment(2L)).thenReturn(sampleDepartment2);

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/stats",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalEmployees")).isEqualTo(3);
        assertThat(response.getBody().get("departmentsWithEmployees")).isEqualTo(2);
        assertThat(response.getBody().get("averageEmployeesPerDepartment")).isEqualTo(1.5);
    }

    // ========================================
    // COUNT BY DEPARTMENT TESTS
    // ========================================

    @Test
    @DisplayName("GET /employees/count - should return employee count by department")
    void countByDepartmentId_ShouldReturnCount() {
        // Arrange
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(2L);

        // Act
        ResponseEntity<Long> response = restTemplate.getForEntity(
                baseUrl + "/count?departmentId=1",
                Long.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(2L);
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @DisplayName("Should handle DepartmentClient failures gracefully")
    void whenDepartmentClientFails_ShouldHandleGracefully() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee1));
        when(departmentClient.getDepartment(1L))
                .thenThrow(new RuntimeException("Department service unavailable"));

        // Act
        ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
                baseUrl + "/1",
                EmployeeDTO.class
        );

        // Assert - Employee should still be returned, just without department info
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFirstName()).isEqualTo("Alice");
        assertThat(response.getBody().getDepartment()).isNull(); // Department should be null due to error
    }

}