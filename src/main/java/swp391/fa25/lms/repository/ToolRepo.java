package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Tool;

import java.util.List;

@Repository
public interface ToolRepo extends JpaRepository<Tool, Long> {
    List<Tool> findByToolNameContainingIgnoreCase(String keyword);
}
