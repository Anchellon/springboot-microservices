package com.example.department.web;


import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import com.example.department.dto.ErrorResponse;
import com.example.department.exception.DepartmentInUseException;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RestController
@RefreshScope
@RequestMapping("/api/v1/departments/")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DepartmentController {
    private final DepartmentService departmentService;

    // ========================================
    // GET /departments - ENHANCED WITH PAGINATION, SORTING, FILTERING
    // ========================================
    @GetMapping
    @Operation(summary = "Get all departments with pagination",
            description = "Retrieve a paginated list of departments with optional filtering by name and code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved departments"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<Page<DepartmentDTO>> all(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'name,asc')", example = "name,asc")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Filter by department name containing text")
            @RequestParam(required = false) String nameContains,
            @Parameter(description = "Filter by department code containing text")
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
    @Operation(summary = "Get department by ID", description = "Retrieve a specific department by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved department"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentDTO> byId(
            @Parameter(description = "Department ID", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("Fetching department with id: {}", id);
        DepartmentDTO department = departmentService.findById(id);
        return ResponseEntity.ok(department);
    }

    // ========================================
    // POST /departments - CREATE NEW DEPARTMENT
    // ========================================
    @PostMapping
    @Operation(summary = "Create a new department", description = "Create a new department with name, code, and optional description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Department created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid department data"),
            @ApiResponse(responseCode = "409", description = "Department with name or code already exists")
    })
    public ResponseEntity<DepartmentDTO> create(
            @Parameter(description = "Department data", required = true)
            @Valid @RequestBody DepartmentDTO createDto
    ) {
        log.info("Creating department: {} ({})", createDto.getName(), createDto.getCode());
        DepartmentDTO createdDepartment = departmentService.create(createDto);
        log.info("Department created with id: {}", createdDepartment.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment);
    }

    // ========================================
    // PUT /departments/{id} - FULL UPDATE
    // ========================================
    @PutMapping("/{id}")
    @Operation(summary = "Update department completely", description = "Replace all department data with new values")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Department updated successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "400", description = "Invalid department data"),
            @ApiResponse(responseCode = "409", description = "Department name or code conflicts with existing department")
    })
    public ResponseEntity<DepartmentDTO> updateDepartment(
            @Parameter(description = "Department ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated department data", required = true)
            @Valid @RequestBody DepartmentDTO updateDto
    ){
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
    @Operation(summary = "Partially update department", description = "Update specific fields of a department")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Department updated successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "409", description = "Department name or code conflicts")
    })
    public ResponseEntity<DepartmentDTO> patchDepartment(
            @Parameter(description = "Department ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Fields to update (null values are ignored)", required = true)
            @Valid @RequestBody DepartmentPatchDTO patchDto
    )  {
        log.info("Partial update of department {}: {}", id, patchDto);

        DepartmentDTO patchedDepartment = departmentService.patchDepartment(id, patchDto);

        log.info("Department {} partially updated", id);
        return ResponseEntity.ok(patchedDepartment);
    }
    @DeleteMapping("/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Department deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "409", description = "Department has employees and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))) // <-- This
    })
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
    @Operation(summary = "Get department by code", description = "Retrieve a department using its unique code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved department"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentDTO> findByCode(
            @Parameter(description = "Department code", required = true, example = "ENG")
            @PathVariable String code
    )  {
        log.info("Fetching department with code: {}", code);

        DepartmentDTO department = departmentService.findByCode(code);

        log.info("Found department: {} (ID: {})", department.getName(), department.getId());
        return ResponseEntity.ok(department);
    }
    @GetMapping("/{id}/employees")
    @Operation(summary = "Get department with employees",
            description = "Retrieve department information along with paginated list of its employees")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved department with employees"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<DepartmentEmployeesDTO> getDepartmentEmployees(
            @Parameter(description = "Department ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria for employees (e.g., 'lastName,asc')", example = "lastName,asc")
            @RequestParam(required = false) String sort
    ){
        log.info("Fetching department {} with employees: page={}, size={}, sort={}", id, page, size, sort);

        DepartmentEmployeesDTO departmentWithEmployees = departmentService.getDepartmentWithEmployees(id, page, size, sort);

        log.info("Returning department '{}' with {} employees on page {}",
                departmentWithEmployees.getDepartment().getName(),
                departmentWithEmployees.getEmployees().getNumberOfElements(),
                departmentWithEmployees.getEmployees().getNumber() + 1);

        return ResponseEntity.ok(departmentWithEmployees);
    }
}