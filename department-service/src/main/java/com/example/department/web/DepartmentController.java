package com.example.department.web;

import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import com.example.department.exception.DepartmentInUseException;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DepartmentController {
    private final DepartmentService departmentService;

    // ========================================
    // GET /departments - ENHANCED WITH PAGINATION, SORTING, FILTERING
    // ========================================
    @GetMapping
    public ResponseEntity<Page<DepartmentDTO>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String nameContains,
            @RequestParam(required = false) String codeContains
    ) {
        log.info("Fetching departments: page={}, size={}, sort={}, nameContains={}, codeContains={}",
                page, size, sort, nameContains, codeContains);

        Page<DepartmentDTO> departments = departmentService.findAll(page, size, sort, nameContains, codeContains);

        log.info("Returning {} departments on page {} of {}",
                departments.getNumberOfElements(), departments.getNumber() + 1, departments.getTotalPages());

        return ResponseEntity.ok(departments);
    }



    // ========================================
    // GET /departments/{id} - SINGLE DEPARTMENT
    // ========================================
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> byId(@PathVariable Long id) {
        log.info("Fetching department with id: {}", id);
        DepartmentDTO department = departmentService.findById(id);
        return ResponseEntity.ok(department);
    }

    // ========================================
    // POST /departments - CREATE NEW DEPARTMENT
    // ========================================
    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody DepartmentDTO createDto) {
        log.info("Creating department: {} ({})", createDto.getName(), createDto.getCode());
        DepartmentDTO createdDepartment = departmentService.create(createDto);
        log.info("Department created with id: {}", createdDepartment.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment);
    }

    // ========================================
    // PUT /departments/{id} - FULL UPDATE
    // ========================================
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDTO> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDTO updateDto
    ) {
        log.info("Full update of department {}: name={}, code={}, description={}",
                id, updateDto.getName(), updateDto.getCode(), updateDto.getDescription());

        DepartmentDTO updatedDepartment = departmentService.updateDepartment(id, updateDto);

        log.info("Department {} fully updated", id);
        return ResponseEntity.ok(updatedDepartment);
    }

    // ========================================
    // PATCH /departments/{id} - PARTIAL UPDATE
    // ========================================
    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentDTO> patchDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentPatchDTO patchDto
    ) {
        log.info("Partial update of department {}: {}", id, patchDto);

        DepartmentDTO patchedDepartment = departmentService.patchDepartment(id, patchDto);

        log.info("Department {} partially updated", id);
        return ResponseEntity.ok(patchedDepartment);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        log.info("Attempting to delete department with id: {}", id);

        try {
            departmentService.deleteDepartment(id);
            log.info("Department {} deleted successfully", id);
            return ResponseEntity.noContent().build(); // 204 No Content

        } catch (DepartmentInUseException e) {
            log.warn("Department deletion blocked: {}", e.getMessage());
            // Exception will be handled by GlobalExceptionHandler
            // Returns 409 Conflict with guidance
            throw e;

        } catch (DepartmentNotFoundException e) {
            log.warn("Department not found for deletion: {}", e.getMessage());
            // Exception will be handled by GlobalExceptionHandler
            // Returns 404 Not Found
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during department deletion: {}", e.getMessage());
            // Exception will be handled by GlobalExceptionHandler
            // Returns 500 Internal Server Error
            throw e;
        }
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<DepartmentDTO> findByCode(@PathVariable String code) {
        log.info("Fetching department with code: {}", code);

        DepartmentDTO department = departmentService.findByCode(code);

        log.info("Found department: {} (ID: {})", department.getName(), department.getId());
        return ResponseEntity.ok(department);
    }
    @GetMapping("/{id}/employees")
    public ResponseEntity<DepartmentEmployeesDTO> getDepartmentEmployees(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        log.info("Fetching department {} with employees: page={}, size={}, sort={}", id, page, size, sort);

        DepartmentEmployeesDTO departmentWithEmployees = departmentService.getDepartmentWithEmployees(id, page, size, sort);

        log.info("Returning department '{}' with {} employees on page {}",
                departmentWithEmployees.getDepartment().getName(),
                departmentWithEmployees.getEmployees().getNumberOfElements(),
                departmentWithEmployees.getEmployees().getNumber() + 1);

        return ResponseEntity.ok(departmentWithEmployees);
    }
}