package swp391.fa25.lms.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Tool;

public interface ToolRepo extends JpaRepository<Tool, Long> {

}
