
package com.example.project.client;

import com.example.project.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

/**
 * Feign client for communicating with Employee Service
 * Maps to your actual Employee Service API endpoints
 */
@FeignClient(name = "EMPLOYEE-SERVICE", path = "/api/v1/employees")
public interface EmployeeServiceClient {

    /**
     * Get a single employee by ID with optional department enrichment
     * Maps to: GET /api/v1/employees/{id}?enrichWithDepartment={enrich}
     *
     * This matches your existing Employee Service endpoint:
     * @GetMapping("/{id}")
     * public ResponseEntity<EmployeeDTO> byId(@PathVariable Long id,
     *                                        @RequestParam(defaultValue = "true") boolean enrichWithDepartment)
     */
    @GetMapping("/api/v1/employees/{id}")
    EmployeeDTO getEmployee(@PathVariable("id") Long id,
                            @RequestParam("enrichWithDepartment") boolean enrichWithDepartment);

    /**
     * Get employee without department details (basic info only)
     * Uses enrichWithDepartment=false for faster response
     */
    default EmployeeDTO getEmployeeBasic(Long id) {
        return getEmployee(id, false);
    }

    /**
     * Get employee with full department details
     * Uses enrichWithDepartment=true for complete information
     */
    default EmployeeDTO getEmployeeWithDepartment(Long id) {
        return getEmployee(id, true);
    }
    /**
     * Search employees by query string
     * Maps to: GET /api/v1/employees/search?q={query}
     *
     * This matches your existing endpoint:
     * @GetMapping("/search")
     * public ResponseEntity<List<EmployeeDTO>> searchEmployees(@RequestParam String q)
     */
    @GetMapping("/api/v1/employees/search")
    List<EmployeeDTO> searchEmployees(@RequestParam("q") String query);

}