package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;

import java.util.List;


@Repository
public interface ToolReportRepository extends JpaRepository<ToolReport, Long>, JpaSpecificationExecutor<ToolReport> {
    boolean existsToolReportByTool(Tool tool);
    List<ToolReport> findToolReportByTool(Tool tool);
    boolean existsByToolAndStatusNot(Tool tool, ToolReport.Status status);

    boolean existsByReporterAndTool(Account reporter, Tool tool);
    List<ToolReport> findByTool_ToolId(Long toolId);
    @Query("SELECT tr FROM ToolReport tr ORDER BY tr.reportedAt ASC")
    List<ToolReport> findAllForStatistics();
    List<ToolReport> findByStatus(ToolReport.Status status);
}