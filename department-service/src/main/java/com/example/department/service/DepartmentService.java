package com.example.department.service;

import com.example.department.domain.Department;
import java.util.List;

public interface DepartmentService {
    List<Department> findAll();
    Department findById(Long id);
    Department create(Department department);
}