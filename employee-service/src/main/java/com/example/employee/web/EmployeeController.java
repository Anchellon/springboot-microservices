package com.example.employee.web;

import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeePatchDTO;
import com.example.employee.dto.EmployeeStatsDTO;
import com.example.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RefreshScope
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Employer Management", description = "Operations related to employer management")
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    @Operation(summary = "Get all employees with pagination", description = "Retrieve a paginated list of employees with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved employees"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<Page<EmployeeDTO>> allPaged(
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'firstName,asc')", example = "firstName,asc") @RequestParam(required = false) String sort,
            @Parameter(description = "Filter by email") @RequestParam(required = false) String email,
            @Parameter(description = "Filter by last name containing") @RequestParam(required = false) String lastNameContains,
            @Parameter(description = "Filter by department ID") @RequestParam(required = false) Long departmentId
    ) {
        log.info("Fetching employees: page={}, size={}, sort={}, email={}, lastNameContains={}, departmentId={}",
                page, size, sort, email, lastNameContains, departmentId);

        Page<EmployeeDTO> employees = service.getAll(page, size, sort, email, lastNameContains, departmentId);

        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Retrieve a specific employee by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved employee"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeDTO> byId(
            @Parameter(description = "Employee ID", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Include department details", example = "true") @RequestParam(defaultValue = "true") boolean enrichWithDepartment
    ) {
        log.info("Fetching employee with id: {}, enrichWithDepartment: {}", id, enrichWithDepartment);
        EmployeeDTO employee = service.getById(id, enrichWithDepartment);
        return ResponseEntity.ok(employee);
    }

    @PostMapping
    @Operation(summary = "Create a new employee", description = "Create a new employee record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid employee data"),
            @ApiResponse(responseCode = "409", description = "Employee already exists")
    })
    public ResponseEntity<EmployeeDTO> create(
            @Parameter(description = "Employee data", required = true) @Valid @RequestBody EmployeeDTO dto,
            @Parameter(description = "Idempotency key for duplicate prevention") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Creating employee with email: {}, idempotencyKey: {}", dto.getEmail(), idempotencyKey);

        EmployeeDTO createdEmployee = service.create(dto, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Update an existing employee record completely")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "400", description = "Invalid employee data"),
            @ApiResponse(responseCode = "409", description = "Business rule violation")
    })
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Updated employee data", required = true) @Valid @RequestBody EmployeeDTO dto
    ) {
        log.info("Updating employee with id: {}, new email: {}", id, dto.getEmail());

        EmployeeDTO updatedEmployee = service.updateEmployee(id, dto);

        return ResponseEntity.ok(updatedEmployee);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update employee", description = "Update specific fields of an employee record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    public ResponseEntity<EmployeeDTO> patchEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Fields to update", required = true) @Valid @RequestBody EmployeePatchDTO patchDto
    ){
        log.info("Partially updating employee {}", id);
        EmployeeDTO updated = service.patchEmployee(id, patchDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee", description = "Delete an employee record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1") @PathVariable Long id
    ) {
        log.info("Deleting employee with id: {}", id);

        service.deleteEmployee(id);

        // 204 No Content - successful deletion with no response body
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/search")
    @Operation(summary = "Search employees", description = "Search employees by query string")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search query")
    })
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(
            @Parameter(description = "Search query", required = true, example = "john") @RequestParam String q
    ) {
        log.info("Searching employees with query: '{}'", q);
        List<EmployeeDTO> employees = service.searchEmployees(q);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get employee statistics", description = "Retrieve statistical information about employees")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<EmployeeStatsDTO> getEmployeeStats() {
        log.info("Fetching employee statistics");

        EmployeeStatsDTO stats = service.getEmployeeStats();

        log.info("Returning stats: {} employees across {} departments",
                stats.getTotalEmployees(), stats.getDepartmentsWithEmployees());

        return ResponseEntity.ok(stats);
    }
    @GetMapping("/count")
    @Operation(summary = "Count employees by department", description = "Get the number of employees in a specific department")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid department ID")
    })
    public ResponseEntity<Long> countByDepartmentId(
            @Parameter(description = "Department ID", required = true, example = "1") @RequestParam Long departmentId
    ) {
        log.info("Counting employees in department: {}", departmentId);
        long count = service.countByDepartmentId(departmentId);
        return ResponseEntity.ok(count);
    }



}