package com.example.employee.web;

import com.example.employee.dto.EmployeeDTO;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> all() {
        log.info("Fetching all employees");
        List<EmployeeDTO> employees = service.getAll();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> byId(@PathVariable Long id) {
        log.info("Fetching employee with id: {}", id);
        EmployeeDTO employee = service.getById(id);
        return ResponseEntity.ok(employee);
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@Valid @RequestBody EmployeeDTO dto) {
        log.info("Creating employee with email: {}", dto.getEmail());
        EmployeeDTO createdEmployee = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }
}