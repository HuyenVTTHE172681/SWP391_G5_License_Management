package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    public List<Feedback> findByTool(Tool tool);
}
