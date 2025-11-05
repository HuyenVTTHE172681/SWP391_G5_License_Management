package swp391.fa25.lms.service.customer;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReport;
import swp391.fa25.lms.repository.FeedbackReportRepository;
import swp391.fa25.lms.repository.FeedbackRepository;

import java.time.LocalDateTime;

@Service
public class FeedbackReportService {
    @Autowired
    private FeedbackRepository feedbackRepo;

    @Autowired
    private FeedbackReportRepository reportRepo;

    @Transactional
    public String reportFeedback(Long feedbackId, Account reporter, FeedbackReport.Reason reason, String description) {
        Feedback fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback không tồn tại."));

        // Không thể tự báo cáo chính mình
        if (fb.getAccount().getAccountId().equals(reporter.getAccountId())) {
            return "Bạn không thể báo cáo feedback của chính mình.";
        }

        // Giới hạn 2 report mỗi ngày
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long countToday = reportRepo.countByReportedByAndReportedAtBetween(reporter, startOfDay, endOfDay);
        if (countToday >= 2) {
            return "Bạn chỉ được báo cáo tối đa 2 feedback mỗi ngày.";
        }

        // Tạo report mới
        FeedbackReport report = new FeedbackReport();
        report.setFeedback(fb);
        report.setReportedBy(reporter);
        report.setStatus(FeedbackReport.Status.PENDING);
        report.setReason(reason);
        report.setDescription(description);
        report.setReportedAt(LocalDateTime.now());

        // Đánh dấu feedback nghi ngờ
        fb.setStatus(Feedback.Status.SUSPECT);

        feedbackRepo.save(fb);
        reportRepo.save(report);

        return "Báo cáo của bạn đã được gửi thành công. Hệ thống sẽ xem xét!";
    }
}
