package com.example.project.repo;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:from IS NULL OR p.startDate >= :from) AND " +
            "(:to IS NULL OR p.endDate <= :to) AND " +
            "(:code IS NULL OR UPPER(p.code) = UPPER(:code)) AND " +
            "(:name IS NULL OR UPPER(p.name) LIKE UPPER(CONCAT('%', :name, '%')))")
    Page<Project> findProjectsWithFilters(
            @Param("status") ProjectStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("code") String code,
            @Param("name") String name,
            Pageable pageable
    );
}