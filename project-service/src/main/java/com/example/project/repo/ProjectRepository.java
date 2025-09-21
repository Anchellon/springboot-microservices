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
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:from IS NULL OR p.startDate >= :from) AND " +
            "(:to IS NULL OR p.endDate <= :to) AND " +
            "(:code IS NULL OR UPPER(CAST(p.code AS string)) = UPPER(CAST(:code AS string))) AND " +
            "(:name IS NULL OR UPPER(CAST(p.name AS string)) LIKE UPPER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Project> findProjectsWithFilters(
            @Param("status") ProjectStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("code") String code,
            @Param("name") String name,
            Pageable pageable
    );

    boolean existsByCodeAndIdNot(String code, Long id);

//    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.members WHERE p.id = :id")
//    Optional<Project> findByIdWithMembers(@Param("id") Long id);

    // FIXED: Return Optional<String> instead of String for better null handling
    @Query("SELECT p.code FROM Project p WHERE p.id = :id")
    Optional<String> findCodeById(@Param("id") Long id);

    @Query("SELECT p.status as label, COUNT(p) as count FROM Project p GROUP BY p.status ORDER BY p.status")
    List<ProjectStatProjection> countByStatus();

    @Query("SELECT DATE_FORMAT(p.startDate, '%Y-%m') as label, COUNT(p) as count " +
            "FROM Project p WHERE p.startDate IS NOT NULL " +
            "GROUP BY DATE_FORMAT(p.startDate, '%Y-%m') " +
            "ORDER BY MIN(p.startDate) DESC")
    List<ProjectStatProjection> countByStartMonth();
}