package com.example.department.service.impl;

import com.example.department.client.EmployeeClient;
import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import com.example.department.dto.EmployeeDTO;
import com.example.department.exception.DepartmentInUseException;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.repo.DepartmentRepository;
import com.example.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;
    private final EmployeeClient employeeClient; // NEW: Add this dependency


    public Page<DepartmentDTO> findAll(int page, int size, String sort,
                                       String nameContains, String codeContains) {
        log.debug("Finding departments: page={}, size={}, sort={}, nameContains={}, codeContains={}",
                page, size, sort, nameContains, codeContains);

        Pageable pageable = createPageable(page, size, sort);

        Page<Department> departmentPage = repository.findWithFilters(
                nameContains, codeContains, pageable
        );

        return departmentPage.map(this::toDTO);
    }

    // EXISTING: Backward compatibility
    @Override
    public List<Department> findAll() {
        log.debug("Finding all departments");
        return repository.findAll();
    }


    @Override
    public DepartmentDTO findById(Long id) {
        log.debug("Finding department with id: {}", id);
        Department department = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id.toString()));
        return toDTO(department);
    }


    @Transactional
    public DepartmentDTO create(DepartmentDTO createDto) {
        log.debug("Creating department: {} ({})", createDto.getName(), createDto.getCode());

        // Check unique name
        if (repository.existsByName(createDto.getName())) {
            throw new DuplicateDepartmentException("name", createDto.getName());
        }

        // Check unique code
        if (repository.existsByCode(createDto.getCode())) {
            throw new DuplicateDepartmentException("code", createDto.getCode());
        }

        Department department = Department.builder()
                .name(createDto.getName())
                .code(createDto.getCode())
                .description(createDto.getDescription())
                .build();

        Department saved = repository.save(department);
        log.info("Department created successfully with id: {} ({})", saved.getId(), saved.getCode());

        return toDTO(saved);
    }

    @Transactional
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO updateDto) {
        log.debug("Updating department with id: {}", id);

        Department existingDepartment = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id.toString()));

        // Check name uniqueness (excluding current department)
        if (!updateDto.getName().equals(existingDepartment.getName()) &&
                repository.existsByNameAndIdNot(updateDto.getName(), id)) {
            throw new DuplicateDepartmentException("name", updateDto.getName());
        }

        // Check code uniqueness (excluding current department)
        if (!updateDto.getCode().equals(existingDepartment.getCode()) &&
                repository.existsByCodeAndIdNot(updateDto.getCode(), id)) {
            throw new DuplicateDepartmentException("code", updateDto.getCode());
        }

        // Full replacement
        existingDepartment.setName(updateDto.getName());
        existingDepartment.setCode(updateDto.getCode());
        existingDepartment.setDescription(updateDto.getDescription());

        Department saved = repository.save(existingDepartment);
        log.info("Department {} fully updated", id);

        return toDTO(saved);
    }
    @Override
    @Transactional
    public DepartmentDTO patchDepartment(Long id, DepartmentPatchDTO patchDto) {
        log.debug("Patching department with id: {}", id);

        // Find existing department
        Department existingDepartment = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id.toString()));

        // Update only provided fields with uniqueness checks
        if (patchDto.getName() != null) {
            if (!patchDto.getName().equals(existingDepartment.getName()) &&
                    repository.existsByNameAndIdNot(patchDto.getName(), id)) {
                throw new DuplicateDepartmentException("name", patchDto.getName());
            }
            log.debug("Updating name: {} → {}", existingDepartment.getName(), patchDto.getName());
            existingDepartment.setName(patchDto.getName());
        }

        if (patchDto.getCode() != null) {
            if (!patchDto.getCode().equals(existingDepartment.getCode()) &&
                    repository.existsByCodeAndIdNot(patchDto.getCode(), id)) {
                throw new DuplicateDepartmentException("code", patchDto.getCode());
            }
            log.debug("Updating code: {} → {}", existingDepartment.getCode(), patchDto.getCode());
            existingDepartment.setCode(patchDto.getCode());
        }

        if (patchDto.getDescription() != null) {
            log.debug("Updating description: {} → {}", existingDepartment.getDescription(), patchDto.getDescription());
            existingDepartment.setDescription(patchDto.getDescription());
        }

        Department saved = repository.save(existingDepartment);
        log.info("Department {} partially updated", id);

        return toDTO(saved);
    }
    @Transactional
    public void deleteDepartment(Long id) {
        log.debug("Attempting to delete department with id: {}", id);

        // STEP 1: Find department (will throw 404 if not found)
        Department department = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id.toString()));

        log.debug("Found department: {} ({})", department.getName(), department.getCode());

        // STEP 2: PROTECTIVE CHECK - Count employees via Employee service
        try {
            long employeeCount = employeeClient.countByDepartmentId(id);
            log.debug("Department {} has {} employees", id, employeeCount);

            if (employeeCount > 0) {
                log.warn("Cannot delete department {} - {} employees still assigned",
                        department.getName(), employeeCount);
                throw new DepartmentInUseException(id, department.getName(), employeeCount);
            }

        } catch (DepartmentInUseException e) {
            throw e; // Re-throw our business exception
        } catch (Exception e) {
            log.error("Failed to check employee count for department {}: {}", id, e.getMessage());
            throw new RuntimeException("Unable to verify department usage before deletion. " +
                    "Please ensure Employee service is available.", e);
        }

        // STEP 3: Safe to delete - no employees assigned
        repository.deleteById(id);
        log.info("Department '{}' ({}) deleted successfully - no employees were assigned",
                department.getName(), department.getCode());
    }

    @Transactional(readOnly = true)
    public DepartmentDTO findByCode(String code) {
        log.debug("Finding department with code: {}", code);

        // Input validation
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Department code cannot be null or empty");
        }

        String trimmedCode = code.trim().toUpperCase(); // Normalize to uppercase
        log.debug("Normalized code: {}", trimmedCode);

        // Find department by code
        Department department = repository.findByCode(trimmedCode)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with code: " + trimmedCode));

        log.debug("Found department: {} (ID: {})", department.getName(), department.getId());
        return toDTO(department);
    }
    @Transactional(readOnly = true)
    public DepartmentEmployeesDTO getDepartmentWithEmployees(Long departmentId, int page, int size, String sort) {
        log.debug("Getting department {} with employees: page={}, size={}, sort={}",
                departmentId, page, size, sort);

        // STEP 1: Get department information
        Department department = repository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId.toString()));

        log.debug("Found department: {} ({})", department.getName(), department.getCode());

        // STEP 2: Get employees via Employee service (with pagination)
        Page<EmployeeDTO> employees;
        try {
            employees = employeeClient.getEmployeesByDepartment(departmentId, page, size, sort);
            log.debug("Retrieved {} employees for department {}",
                    employees.getNumberOfElements(), department.getName());

        } catch (Exception e) {
            log.error("Failed to retrieve employees for department {}: {}", departmentId, e.getMessage());
            throw new RuntimeException("Unable to retrieve employees for this department. " +
                    "Please ensure Employee service is available.", e);
        }

        // STEP 3: Build composed response
        String summary = String.format("Department '%s' has %d employees",
                department.getName(), employees.getTotalElements());

        DepartmentEmployeesDTO response = DepartmentEmployeesDTO.builder()
                .department(toDTO(department))
                .employees(employees)
                .totalEmployees(employees.getTotalElements())
                .summary(summary)
                .build();

        log.info("Composed response for department {}: {} total employees",
                department.getName(), employees.getTotalElements());

        return response;
    }


    private Pageable createPageable(int page, int size, String sort) {
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String property = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, property));
        }
        return PageRequest.of(page, size);
    }

    private DepartmentDTO toDTO(Department department) {
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .build();
    }
}

