package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;

public interface FeedBackRepo extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByTool(Tool tool, Pageable pageable);
    long countByTool(Tool tool);
    @Query("select coalesce(avg(f.rating), 0) from Feedback f where f.tool = :tool")
    double averageRating(@Param("tool") Tool tool);
}
