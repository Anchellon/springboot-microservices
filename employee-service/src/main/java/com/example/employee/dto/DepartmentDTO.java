package com.example.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Department data transfer object")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDTO {
    @Schema(description = "Department ID", example = "1")
    private Long id;

    @Schema(description = "Department name", example = "Engineering")
    private String name;

    @Schema(description = "Department description", example = "Software development team")
    private String description;
}