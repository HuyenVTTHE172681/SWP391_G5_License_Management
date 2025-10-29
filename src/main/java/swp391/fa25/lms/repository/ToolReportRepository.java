package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.ToolReport;

@Repository
public interface ToolReportRepository extends JpaRepository<ToolReport, Long> {
}
