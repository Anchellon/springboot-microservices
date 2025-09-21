
package com.example.project.client;

import com.example.project.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(name = "employee-service", url = "${app.services.employee.url}")
public interface EmployeeServiceClient {

    @GetMapping("/api/v1/employees/{id}")
    EmployeeDTO getEmployee(@PathVariable Long id);

    @GetMapping("/api/v1/employees/exists/{id}")
    Boolean employeeExists(@PathVariable Long id);

    @GetMapping("/api/v1/employees/batch")
    List<EmployeeDTO> getEmployeesBatch(@RequestParam Set<Long> ids);
}