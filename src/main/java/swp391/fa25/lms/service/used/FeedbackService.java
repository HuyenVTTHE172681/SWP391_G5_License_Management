package swp391.fa25.lms.service.used;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.util.List;

@Service
public class FeedbackService {

    @Autowired
    FeedbackRepository feedbackRepository;

}