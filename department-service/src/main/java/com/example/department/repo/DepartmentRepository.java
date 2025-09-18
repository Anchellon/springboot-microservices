package com.example.department.repo;

import com.example.department.domain.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String name);

    boolean existsByCode(String code);

    // NEW: For update validation (exclude current department)
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByCodeAndIdNot(String code, Long id);

    // NEW: Advanced filtering with pagination
    @Query("SELECT d FROM Department d WHERE " +
            "(:nameContains IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :nameContains, '%'))) AND " +
            "(:codeContains IS NULL OR LOWER(d.code) LIKE LOWER(CONCAT('%', :codeContains, '%')))")
    Page<Department> findWithFilters(
            @Param("nameContains") String nameContains,
            @Param("codeContains") String codeContains,
            Pageable pageable
    );
    Optional<Department> findByCode(String code);
}