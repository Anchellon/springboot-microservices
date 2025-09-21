package com.example.project.mapper;

import com.example.project.domain.Project;
import com.example.project.dto.ProjectDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectDTO toDTO(Project project);
}