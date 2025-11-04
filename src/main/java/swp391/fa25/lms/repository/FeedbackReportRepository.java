package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.FeedbackReport;

import java.time.LocalDateTime;

@Repository
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, Long> {
    // Count Feedback report
    long countByReportedByAndReportedAtBetween(Account acc, LocalDateTime start, LocalDateTime end);
}
