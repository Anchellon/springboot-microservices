package com.example.department.service.impl;

import com.example.department.domain.Department;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.repo.DepartmentRepository;
import com.example.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;

    @Override
    public List<Department> findAll() {
        log.debug("Finding all departments");
        return repository.findAll();
    }

    @Override
    public Department findById(Long id) {
        log.debug("Finding department with id: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id.toString()));
    }

    @Override
    @Transactional
    public Department create(Department department) {
        log.debug("Creating department: {}", department.getName());

        // Example business rule: Check for duplicate name
        if (repository.existsByName(department.getName())) {
            throw new DuplicateDepartmentException("name", department.getName());
        }

        return repository.save(department);
    }
}
