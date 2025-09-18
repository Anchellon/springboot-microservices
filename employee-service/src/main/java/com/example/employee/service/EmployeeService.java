package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeePatchDTO;
import com.example.employee.dto.EmployeeStatsDTO;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentClient departmentClient;
    private final IdempotencyService idempotencyService; // NEW: Add this dependency


    public List<EmployeeDTO> getAll() {
        log.debug("Fetching all employees as list");
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    public Page<EmployeeDTO> getAll(int page, int size, String sort,
                                    String email, String lastNameContains, Long departmentId) {
        log.debug("Fetching employees: page={}, size={}, sort={}, email={}, lastNameContains={}, departmentId={}",
                page, size, sort, email, lastNameContains, departmentId);

        // Create Pageable object with sorting
        Pageable pageable = createPageable(page, size, sort);

        // Use new filtering method
        Page<Employee> employeePage = repository.findWithFilters(
                email, lastNameContains, departmentId, pageable
        );

        // Convert Page<Employee> to Page<EmployeeDTO>
        return employeePage.map(this::toDTO);
    }
    public EmployeeDTO getById(Long id, boolean enrichWithDepartment) {
        log.debug("Fetching employee with id: {}, enrichWithDepartment: {}", id, enrichWithDepartment);

        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id.toString()));

        return toDTO(employee, enrichWithDepartment);
    }

    public EmployeeDTO getById(Long id) {
        log.debug("Fetching employee with id: {}", id);
        Employee e = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id.toString()));
        return toDTO(e);
    }
    // UPDATED: Enhanced create method with idempotency support
    @Transactional
    public EmployeeDTO create(EmployeeDTO dto, String idempotencyKey) {
        log.debug("Creating employee with email: {}, idempotencyKey: {}", dto.getEmail(), idempotencyKey);

        // STEP 1: Check idempotency first (before any business logic)
        if (idempotencyKey != null && idempotencyService.isProcessed(idempotencyKey)) {
            log.info("Idempotent request detected for key: {}", idempotencyKey);
            EmployeeDTO cachedResult = (EmployeeDTO) idempotencyService.getResult(idempotencyKey);
            log.info("Returning cached result for employee: {}", cachedResult.getId());
            return cachedResult;
        }

        // STEP 2: Business rule - Check for duplicate email
        if (repository.existsByEmail(dto.getEmail())) {
            log.warn("Attempted to create employee with duplicate email: {}", dto.getEmail());
            throw new DuplicateEmployeeException("email", dto.getEmail());
        }

        // STEP 3: Create the employee
        Employee e = Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .departmentId(dto.getDepartmentId())
                .build();

        e = repository.save(e);
        log.info("Employee created successfully with id: {}", e.getId());

        // STEP 4: Convert to DTO
        EmployeeDTO result = toDTO(e);

        // STEP 5: Store idempotency result (after successful creation)
        if (idempotencyKey != null) {
            idempotencyService.storeResult(idempotencyKey, result);
            log.debug("Stored idempotency result for key: {}", idempotencyKey);
        }

        return result;
    }
    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
        return create(dto, null); // No idempotency key
    }

    @Transactional
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        log.debug("Updating employee with id: {}, new email: {}", id, dto.getEmail());

        // STEP 1: Check if employee exists
        Employee existingEmployee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id.toString()));

        log.debug("Found existing employee: {} {}", existingEmployee.getFirstName(), existingEmployee.getLastName());

        // STEP 2: Check email uniqueness (excluding current employee)
        if (repository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            log.warn("Attempted to update employee {} to duplicate email: {}", id, dto.getEmail());
            throw new DuplicateEmployeeException("email", dto.getEmail());
        }

        // STEP 3: Full replacement of all fields
        existingEmployee.setFirstName(dto.getFirstName());
        existingEmployee.setLastName(dto.getLastName());
        existingEmployee.setEmail(dto.getEmail());
        existingEmployee.setDepartmentId(dto.getDepartmentId());

        // STEP 4: Save updated employee
        Employee updatedEmployee = repository.save(existingEmployee);
        log.info("Employee updated successfully with id: {}", updatedEmployee.getId());

        return toDTO(updatedEmployee);
    }

    @Transactional
    public EmployeeDTO patchEmployee(Long id, EmployeePatchDTO patchDto) {
        Employee existing = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id.toString()));

        // Only update provided fields
        if (patchDto.getFirstName() != null) {
            existing.setFirstName(patchDto.getFirstName());
        }
        if (patchDto.getLastName() != null) {
            existing.setLastName(patchDto.getLastName());
        }
        if (patchDto.getEmail() != null) {
            // Check uniqueness only if email is changing
            if (repository.existsByEmailAndIdNot(patchDto.getEmail(), id)) {
                throw new DuplicateEmployeeException("email", patchDto.getEmail());
            }
            existing.setEmail(patchDto.getEmail());
        }
        if (patchDto.getDepartmentId() != null) {
            existing.setDepartmentId(patchDto.getDepartmentId());
        }

        return toDTO(repository.save(existing));
    }
    @Transactional
    public void deleteEmployee(Long id) {
        log.debug("Deleting employee with id: {}", id);

        // STEP 1: Check if employee exists (throw 404 if not found)
        if (!repository.existsById(id)) {
            log.warn("Attempted to delete non-existent employee with id: {}", id);
            throw new EmployeeNotFoundException(id.toString());
        }

        // STEP 2: Delete the employee
        repository.deleteById(id);

        log.info("Employee with id: {} deleted successfully", id);

        // Note: Method returns void - no data to return for 204 response
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> searchEmployees(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        List<Employee> employees = repository.searchByNameOrEmail(searchTerm.trim());
        return employees.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeStatsDTO getEmployeeStats() {
        log.debug("Calculating employee statistics");

        // STEP 1: Get basic counts
        long totalEmployees = repository.count();
        log.debug("Total employees: {}", totalEmployees);

        // STEP 2: Get department breakdown
        List<Object[]> departmentCounts = repository.countByDepartment();
        log.debug("Department breakdown query returned {} departments", departmentCounts.size());

        // STEP 3: Convert to Map<departmentId, count>
        Map<Long, Long> employeesByDepartment = departmentCounts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // departmentId
                        row -> (Long) row[1]            // employee count
                ));

        // STEP 4: Enrich with department names (via Feign calls)
        Map<String, Long> employeesByDepartmentName = new HashMap<>();
        for (Map.Entry<Long, Long> entry : employeesByDepartment.entrySet()) {
            Long departmentId = entry.getKey();
            Long employeeCount = entry.getValue();

            try {
                DepartmentDTO department = departmentClient.getDepartment(departmentId);
                employeesByDepartmentName.put(department.getName(), employeeCount);
                log.debug("Department {}: {} employees", department.getName(), employeeCount);
            } catch (Exception e) {
                // Graceful degradation - use department ID if name fetch fails
                employeesByDepartmentName.put("Department " + departmentId, employeeCount);
                log.warn("Failed to fetch name for department {}: {}", departmentId, e.getMessage());
            }
        }

        // STEP 5: Calculate additional metrics
        long departmentsWithEmployees = repository.countDistinctDepartments();
        double averageEmployeesPerDepartment = departmentsWithEmployees > 0
                ? (double) totalEmployees / departmentsWithEmployees
                : 0.0;

        // STEP 6: Build response
        EmployeeStatsDTO stats = new EmployeeStatsDTO();
        stats.setTotalEmployees(totalEmployees);
        stats.setEmployeesByDepartment(employeesByDepartment);
        stats.setEmployeesByDepartmentName(employeesByDepartmentName);
        stats.setDepartmentsWithEmployees(departmentsWithEmployees);
        stats.setAverageEmployeesPerDepartment(Math.round(averageEmployeesPerDepartment * 100.0) / 100.0);

        log.info("Stats calculated: {} total employees across {} departments",
                totalEmployees, departmentsWithEmployees);

        return stats;
    }
    @Transactional(readOnly = true)
    public long countByDepartmentId(Long departmentId) {
        log.debug("Counting employees in department: {}", departmentId);
        long count = repository.countByDepartmentId(departmentId);
        log.debug("Department {} has {} employees", departmentId, count);
        return count;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getEmployeesByDepartment(Long departmentId, int page, int size, String sort) {
        log.debug("Getting employees for department {}: page={}, size={}, sort={}", departmentId, page, size, sort);

        // Use your existing filtering method with departmentId filter
        Page<EmployeeDTO> employees = getAll(page, size, sort, null, null, departmentId);

        log.debug("Found {} employees in department {}", employees.getTotalElements(), departmentId);
        return employees;
    }

    private EmployeeDTO toDTO(Employee e, boolean enrichWithDepartment) {
        DepartmentDTO dept = null;

        if (enrichWithDepartment && e.getDepartmentId() != null) {
            try {
                dept = departmentClient.getDepartment(e.getDepartmentId());
                log.debug("Successfully enriched employee {} with department {}", e.getId(), dept.getName());
            } catch (Exception ex) {
                log.warn("Failed to fetch department {} for employee {}: {}",
                        e.getDepartmentId(), e.getId(), ex.getMessage());
            }
        } else if (!enrichWithDepartment) {
            log.debug("Skipping department enrichment for employee {}", e.getId());
        }

        return EmployeeDTO.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .departmentId(e.getDepartmentId())
                .department(dept) // Will be null if not enriched or if enrichment failed
                .build();
    }

    private EmployeeDTO toDTO(Employee e) {
        return toDTO(e, true); // Default: always enrich
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
}