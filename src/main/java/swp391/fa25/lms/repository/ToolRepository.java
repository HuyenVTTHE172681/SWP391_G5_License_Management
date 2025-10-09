package swp391.fa25.lms.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import swp391.fa25.lms.model.Tool;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {

    List<Tool> findAllByToolNameContainingIgnoreCaseAndCategory_CategoryId(
            String toolName, Long categoryId, Sort sort);

    List<Tool> findAllByToolNameContainingIgnoreCase(String toolName, Sort sort);

    List<Tool> findAll(Sort sort);

    List<Tool> findAllByCategory_CategoryId(Long categoryId, Sort sort);
}
