package com.example.department.web;

import com.example.department.domain.Department;
import com.example.department.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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

    @GetMapping
    public ResponseEntity<List<Department>> all() {
        log.info("Fetching all departments");
        List<Department> departments = departmentService.findAll();
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> byId(@PathVariable Long id) {
        log.info("Fetching department with id: {}", id);
        Department department = departmentService.findById(id);
        return ResponseEntity.ok(department);
    }

    @PostMapping
    public ResponseEntity<Department> create(@Valid @RequestBody Department department) {
        log.info("Creating department: {}", department.getName());
        Department createdDepartment = departmentService.create(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment);
    }
}