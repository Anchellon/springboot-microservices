package com.example.project.mapper;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectMember;
import com.example.project.dto.ProjectDTO;
import com.example.project.dto.ProjectMemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // Project mappings
    ProjectDTO toDTO(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "members", ignore = true)
    Project toEntity(ProjectDTO projectDTO);

    // ProjectMember mappings
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(target = "employee", ignore = true) // Set manually when enriching
    ProjectMemberDTO memberToDTO(ProjectMember projectMember);


}