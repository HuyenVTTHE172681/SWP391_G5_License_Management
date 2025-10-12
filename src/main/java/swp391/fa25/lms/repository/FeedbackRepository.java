package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    public List<Feedback> findByTool(Tool tool);
    Page<Feedback> findByTool(Tool tool, Pageable pageable);
    long countByTool(Tool tool);
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Double findAverageRatingByTool(@Param("toolId") Long toolId);
}
