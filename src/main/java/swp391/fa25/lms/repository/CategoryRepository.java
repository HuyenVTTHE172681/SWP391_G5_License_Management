package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;

import java.util.List;

public interface ToolRepo extends JpaRepository<Tool, Long> {
    List<Tool> findBySeller(Account seller);
    List<Tool> findByToolNameContainingIgnoreCase(String keyword);
}
