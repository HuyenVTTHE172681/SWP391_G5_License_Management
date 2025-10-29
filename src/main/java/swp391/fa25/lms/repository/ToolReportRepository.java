package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.ToolReport;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToolReportRepository extends JpaRepository<ToolReport, Long>, JpaSpecificationExecutor<ToolReport> {
//    @Query("""
//        SELECT tr FROM ToolReport tr
//        WHERE
//            (:status IS NULL OR tr.status = :status)
//        AND (:toolId IS NULL OR tr.tool.toolId = :toolId)
//        AND (:reporterId IS NULL OR tr.reporter.accountId = :reporterId)
//        AND (:fromDate IS NULL OR tr.reportedAt >= :fromDate)
//        AND (:toDate IS NULL OR tr.reportedAt <= :toDate)
//        ORDER BY tr.reportedAt DESC
//        """)
//    List<ToolReport> filterReports(
//            @Param("status") ToolReport.Status status,
//            @Param("toolId") Long toolId,
//            @Param("reporterId") Long reporterId,
//            @Param("fromDate") LocalDateTime fromDate,
//            @Param("toDate") LocalDateTime toDate
//    );
}
