package com.example.department.client;

import com.example.department.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "EMPLOYEE-SERVICE", path = "/api/v1/employees")
public interface EmployeeClient {

    // Call Employee service to count employees in this department
    @GetMapping("/count")
    long countByDepartmentId(@RequestParam("departmentId") Long departmentId);

    @GetMapping
    Page<EmployeeDTO> getEmployeesByDepartment(
            @RequestParam("departmentId") Long departmentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort
    );
}