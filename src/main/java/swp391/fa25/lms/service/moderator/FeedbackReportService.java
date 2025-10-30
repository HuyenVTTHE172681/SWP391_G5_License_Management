package swp391.fa25.lms.service.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.FeedbackReport;
import swp391.fa25.lms.repository.FeedbackReportRepository;

import java.util.List;

@Service("moderator")
public class FeedbackReportService {
    @Autowired
    private FeedbackReportRepository feedbackReportRepository;
    public List<FeedbackReport> findAll(){
        return feedbackReportRepository.findAll();
    }
}
