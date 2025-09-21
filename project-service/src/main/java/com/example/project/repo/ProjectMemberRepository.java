package  com.example.project.repo;
import com.example.project.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(Long projectId);

    Optional<ProjectMember> findByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    void deleteByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    long countByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}