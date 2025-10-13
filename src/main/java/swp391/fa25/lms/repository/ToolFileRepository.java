package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolFile;

import java.util.List;

public interface ToolFileRepository extends JpaRepository<ToolFile, Integer> {
    List<ToolFile> findByTool(Tool tool);
}
