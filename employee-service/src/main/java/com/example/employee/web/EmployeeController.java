package com.example.employee.web;

import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeePatchDTO;
import com.example.employee.dto.EmployeeStatsDTO;
import com.example.employee.service.EmployeeService;
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
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    public ResponseEntity<Page<EmployeeDTO>> allPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String lastNameContains,
            @RequestParam(required = false) Long departmentId
    ) {
        log.info("Fetching employees: page={}, size={}, sort={}, email={}, lastNameContains={}, departmentId={}",
                page, size, sort, email, lastNameContains, departmentId);

        Page<EmployeeDTO> employees = service.getAll(page, size, sort, email, lastNameContains, departmentId);

        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> byId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean enrichWithDepartment
    ) {
        log.info("Fetching employee with id: {}, enrichWithDepartment: {}", id, enrichWithDepartment);
        EmployeeDTO employee = service.getById(id, enrichWithDepartment);
        return ResponseEntity.ok(employee);
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> create(
            @Valid @RequestBody EmployeeDTO dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Creating employee with email: {}, idempotencyKey: {}", dto.getEmail(), idempotencyKey);

        EmployeeDTO createdEmployee = service.create(dto, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDTO dto
    ) {
        log.info("Updating employee with id: {}, new email: {}", id, dto.getEmail());

        EmployeeDTO updatedEmployee = service.updateEmployee(id, dto);

        return ResponseEntity.ok(updatedEmployee);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeDTO> patchEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeePatchDTO patchDto
    ) {
        log.info("Partially updating employee {}", id);
        EmployeeDTO updated = service.patchEmployee(id, patchDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        log.info("Deleting employee with id: {}", id);

        service.deleteEmployee(id);

        // 204 No Content - successful deletion with no response body
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(@RequestParam String q) {
        log.info("Searching employees with query: '{}'", q);
        List<EmployeeDTO> employees = service.searchEmployees(q);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/stats")
    public ResponseEntity<EmployeeStatsDTO> getEmployeeStats() {
        log.info("Fetching employee statistics");

        EmployeeStatsDTO stats = service.getEmployeeStats();

        log.info("Returning stats: {} employees across {} departments",
                stats.getTotalEmployees(), stats.getDepartmentsWithEmployees());

        return ResponseEntity.ok(stats);
    }
    @GetMapping("/count")
    public ResponseEntity<Long> countByDepartmentId(@RequestParam Long departmentId) {
        log.info("Counting employees in department: {}", departmentId);
        long count = service.countByDepartmentId(departmentId);
        return ResponseEntity.ok(count);
    }



}