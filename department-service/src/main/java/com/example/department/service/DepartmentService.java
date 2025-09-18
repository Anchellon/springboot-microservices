package com.example.department.service;

import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentEmployeesDTO;
import com.example.department.dto.DepartmentPatchDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DepartmentService {
    List<Department> findAll();
    DepartmentDTO findById(Long id);
    DepartmentDTO create(DepartmentDTO createDto);

    DepartmentDTO updateDepartment(Long id, @Valid DepartmentDTO updateDto);

    DepartmentDTO patchDepartment(Long id, DepartmentPatchDTO patchDto);

    Page<DepartmentDTO> findAll(int page, int size, String sort, String nameContains, String codeContains);

    void deleteDepartment(Long id);

    DepartmentDTO findByCode(String code);

    DepartmentEmployeesDTO getDepartmentWithEmployees(Long id, int page, int size, String sort);
}