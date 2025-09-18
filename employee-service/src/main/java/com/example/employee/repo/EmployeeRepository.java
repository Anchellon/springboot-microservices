package com.example.employee.repo;

import com.example.employee.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmail(String email);

    @Query("SELECT e FROM Employee e WHERE " +
            "(:email IS NULL OR LOWER(e.email) = LOWER(:email)) AND " +
            "(:lastNameContains IS NULL OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :lastNameContains, '%'))) AND " +
            "(:departmentId IS NULL OR e.departmentId = :departmentId)")
    Page<Employee> findWithFilters(
            @Param("email") String email,
            @Param("lastNameContains") String lastNameContains,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );

    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchByNameOrEmail(@Param("searchTerm") String searchTerm);

    // NEW: Check if email exists for a different employee (excluding current ID)
    boolean existsByEmailAndIdNot(String email, Long id);


    @Query("SELECT e.departmentId, COUNT(e) FROM Employee e GROUP BY e.departmentId")
    List<Object[]> countByDepartment();

    // NEW: Count distinct departments that have employees
    @Query("SELECT COUNT(DISTINCT e.departmentId) FROM Employee e WHERE e.departmentId IS NOT NULL")
    long countDistinctDepartments();

}
