package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentClient departmentClient;

    public List<EmployeeDTO> getAll() {
        log.debug("Fetching all employees");
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EmployeeDTO getById(Long id) {
        log.debug("Fetching employee with id: {}", id);
        Employee e = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id.toString()));
        return toDTO(e);
    }

    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
        log.debug("Creating employee with email: {}", dto.getEmail());

        // Business rule: Check for duplicate email
        if (repository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEmployeeException("email", dto.getEmail());
        }

        Employee e = Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .departmentId(dto.getDepartmentId())
                .build();

        e = repository.save(e);
        log.info("Employee created successfully with id: {}", e.getId());

        return toDTO(e);
    }

    private EmployeeDTO toDTO(Employee e) {
        DepartmentDTO dept = null;
        if (e.getDepartmentId() != null) {
            try {
                dept = departmentClient.getDepartment(e.getDepartmentId());
            } catch (Exception ex) {
                log.warn("Failed to fetch department {} for employee {}: {}",
                        e.getDepartmentId(), e.getId(), ex.getMessage());
            }
        }

        return EmployeeDTO.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .departmentId(e.getDepartmentId())
                .department(dept)
                .build();
    }
}