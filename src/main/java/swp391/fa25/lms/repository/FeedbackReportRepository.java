package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.FeedbackReport;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, Long> {
    // Count Feedback report
    long countByReportedByAndReportedAtBetween(Account acc, LocalDateTime start, LocalDateTime end);
    List<FeedbackReport> findByStatus(FeedbackReport.Status status);
    @Query("""
           SELECT fr
           FROM FeedbackReport fr
           WHERE (:reporterId IS NULL OR fr.reportedBy.accountId = :reporterId)
             AND (:reason     IS NULL OR fr.reason = :reason)
             AND (:status     IS NULL OR fr.status = :status)
             AND (:fromDate   IS NULL OR fr.reportedAt >= :fromDate)
             AND (:toDate     IS NULL OR fr.reportedAt <= :toDate)
           ORDER BY fr.reportedAt DESC
           """)
    List<FeedbackReport> search(
            @Param("reporterId") Long reporterId,
            @Param("reason")     FeedbackReport.Reason reason,
            @Param("status")     FeedbackReport.Status status,
            @Param("fromDate")   LocalDateTime fromDate,
            @Param("toDate")     LocalDateTime toDate,
            Pageable pageable  // có thể truyền Pageable.unpaged()
    );
}
