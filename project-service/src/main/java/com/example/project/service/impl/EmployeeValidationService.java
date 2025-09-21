package com.example.project.service.impl;

import com.example.project.client.EmployeeServiceClient;
import com.example.project.dto.EmployeeDTO;
import com.example.project.exception.ExternalServiceException;
import com.example.project.exception.ExternalServiceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeValidationService {

    private final EmployeeServiceClient employeeServiceClient;

    /**
     * Get basic employee info (for member enrichment when enrich=true)
     * Calls: GET /api/v1/employees/{id}?enrichWithDepartment=false
     * Returns: { id, firstName, lastName, email } - minimal employee snapshot
     */
    @Cacheable(value = "employee-basic", key = "#employeeId", unless = "#result == null")
    public EmployeeDTO getEmployeeBasic(Long employeeId) {
        log.debug("Getting basic employee info: {}", employeeId);

        try {
            // Call your Employee Service with enrichWithDepartment=false for faster response
            EmployeeDTO employee = employeeServiceClient.getEmployeeBasic(employeeId);
            log.debug("Successfully retrieved basic employee {}: {} {}",
                    employeeId, employee.getFirstName(), employee.getLastName());
            return employee;

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Employee not found during basic enrichment: {}", employeeId);
            throw new ExternalServiceNotFoundException("Employee not found", "Employee Service");

        } catch (HttpServerErrorException ex) {
            log.error("Employee Service server error for employee {}: HTTP {}",
                    employeeId, ex.getStatusCode());
            throw new ExternalServiceException(
                    "Employee service temporarily unavailable",
                    "Employee Service",
                    ex
            );

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Employee Service for employee {}: {}",
                    employeeId, ex.getMessage());
            throw new ExternalServiceException(
                    "Unable to connect to Employee service",
                    "Employee Service",
                    ex
            );
        } catch (Exception ex) {
            log.error("Unexpected error getting basic employee info for {}: {}",
                    employeeId, ex.getMessage(), ex);
            throw new ExternalServiceException(
                    "Unexpected error communicating with Employee service",
                    "Employee Service",
                    ex
            );
        }
    }

    /**
     * Get basic employee info for multiple employees (for member list enrichment)
     * This is what gets called when enrich=true in getProjectMembers()
     */
    public List<EmployeeDTO> getEmployeesBasic(Set<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            log.debug("No employees to enrich - returning empty list");
            return List.of();
        }

        log.debug("Getting basic info for {} employees: {}", employeeIds.size(), employeeIds);

        try {
            List<EmployeeDTO> employees = employeeIds.stream()
                    .map(this::getEmployeeBasic) // This method is cached
                    .collect(Collectors.toList());

            log.debug("Successfully retrieved basic info for {} employees", employees.size());
            return employees;

        } catch (ExternalServiceNotFoundException | ExternalServiceException ex) {
            log.warn("Batch employee basic enrichment failed: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Validate that a single employee exists (for adding members to projects)
     * Calls: GET /api/v1/employees/{id}?enrichWithDepartment=false
     */
    public EmployeeDTO validateEmployeeExists(Long employeeId) {
        log.debug("Validating employee exists: {}", employeeId);

        try {
            EmployeeDTO employee = employeeServiceClient.getEmployeeBasic(employeeId);
            log.debug("Employee {} validation successful: {} {}",
                    employeeId, employee.getFirstName(), employee.getLastName());
            return employee;

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Employee validation failed - not found: {}", employeeId);
            throw new ExternalServiceNotFoundException("Employee not found", "Employee Service");

        } catch (HttpServerErrorException ex) {
            log.error("Employee Service server error during validation for employee {}: HTTP {}",
                    employeeId, ex.getStatusCode());
            throw new ExternalServiceException(
                    "Employee service temporarily unavailable",
                    "Employee Service",
                    ex
            );

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Employee Service during validation for employee {}: {}",
                    employeeId, ex.getMessage());
            throw new ExternalServiceException(
                    "Unable to connect to Employee service",
                    "Employee Service",
                    ex
            );
        } catch (Exception ex) {
            log.error("Unexpected error during employee validation for employee {}: {}",
                    employeeId, ex.getMessage(), ex);
            throw new ExternalServiceException(
                    "Unexpected error communicating with Employee service",
                    "Employee Service",
                    ex
            );
        }
    }

    /**
     * Validate multiple employees exist (for adding multiple members)
     */
    public List<EmployeeDTO> validateEmployeesExist(Set<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            log.debug("No employees to validate - returning empty list");
            return List.of();
        }

        log.debug("Validating {} employees exist: {}", employeeIds.size(), employeeIds);

        try {
            List<EmployeeDTO> employees = employeeIds.stream()
                    .map(this::validateEmployeeExists)
                    .collect(Collectors.toList());

            log.debug("Successfully validated {} employees", employees.size());
            return employees;

        } catch (ExternalServiceNotFoundException | ExternalServiceException ex) {
            log.warn("Batch employee validation failed: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Get employee with full department details (if needed in the future)
     * Calls: GET /api/v1/employees/{id}?enrichWithDepartment=true
     */
    @Cacheable(value = "employee-details", key = "#employeeId", unless = "#result == null")
    public EmployeeDTO getEmployeeWithDepartment(Long employeeId) {
        log.debug("Getting employee with department details: {}", employeeId);

        try {
            return employeeServiceClient.getEmployeeWithDepartment(employeeId);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Employee not found for full enrichment: {}", employeeId);
            throw new ExternalServiceNotFoundException("Employee not found", "Employee Service");
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            log.error("Employee service error for employee {}: {}", employeeId, ex.getMessage());
            throw new ExternalServiceException("Employee service temporarily unavailable", "Employee Service", ex);
        }
    }

    /**
     * Check Employee Service availability (for health checks)
     */
    public boolean isEmployeeServiceAvailable() {
        try {
            // Use search endpoint as a lightweight health check
            employeeServiceClient.searchEmployees("test");
            return true;
        } catch (Exception ex) {
            log.warn("Employee Service availability check failed: {}", ex.getMessage());
            return false;
        }
    }
}
