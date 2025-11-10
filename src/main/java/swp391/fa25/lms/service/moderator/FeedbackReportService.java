package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReport;
import swp391.fa25.lms.repository.FeedbackReportRepository;
import swp391.fa25.lms.repository.FeedbackRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service("moderator")
public class FeedbackReportService {
    @Autowired
    private FeedbackReportRepository feedbackReportRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;
    public List<FeedbackReport> findAll() {
        return feedbackReportRepository.findAll();
    }

    public List<FeedbackReport> findAllByStatus(FeedbackReport.Status status) {
        return feedbackReportRepository.findByStatus(status);
    }

    public List<FeedbackReport> findAll(Long reporterId,
                                        FeedbackReport.Reason reason,
                                        LocalDate fromDate, LocalDate toDate,
                                        FeedbackReport.Status status) {
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.atTime(LocalTime.MAX) : null;

        return feedbackReportRepository.search(reporterId, reason, status, from, to, Pageable.unpaged());
    }

    public void approve(Long id) {
        FeedbackReport fr = feedbackReportRepository.findById(id).orElseThrow();
        fr.setStatus(FeedbackReport.Status.APPROVED);
        feedbackRepository.findByFeedbackId(fr.getFeedback().getFeedbackId()).setStatus(Feedback.Status.HIDDEN);
        feedbackReportRepository.save(fr);
        feedbackRepository.save(fr.getFeedback());
    }
    public void reject(Long id) {
        FeedbackReport fr = feedbackReportRepository.findById(id).orElseThrow();
        fr.setStatus(FeedbackReport.Status.REJECTED);
        feedbackRepository.findByFeedbackId(fr.getFeedback().getFeedbackId()).setStatus(Feedback.Status.PUBLISHED);
        feedbackReportRepository.save(fr);
        feedbackRepository.save(fr.getFeedback());
    }
}
