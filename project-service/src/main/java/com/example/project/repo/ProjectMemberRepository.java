package com.example.project.repo;

import com.example.project.domain.ProjectMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // CONSISTENT: Use project.id navigation for all methods
    Page<ProjectMember> findByProject_Id(Long projectId, Pageable pageable);
    Optional<ProjectMember> findByProject_IdAndEmployeeId(Long projectId, Long employeeId);
    boolean existsByProject_IdAndEmployeeId(Long projectId, Long employeeId);
    long countByProject_Id(Long projectId);

    @Modifying
    @Transactional
    void deleteByProject_IdAndEmployeeId(Long projectId, Long employeeId);

    @Modifying
    @Transactional
    void deleteByProject_Id(Long projectId);

    // FIXED: Use @Query for field projection
    @Query("SELECT pm.employeeId FROM ProjectMember pm WHERE pm.project.id = ?1")
    List<Long> findEmployeeIdsByProjectId(Long projectId);


}