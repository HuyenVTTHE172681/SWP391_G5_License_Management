package swp391.fa25.lms.controller.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swp391.fa25.lms.service.moderator.FeedbackReportService;

@Controller
@RequestMapping("/moderator/feedback")
public class ModeratorFeedbackController {
    @Autowired
    @Qualifier("moderator")
    private FeedbackReportService feedbackReportService;

    @GetMapping({"/list", ""})
    public String feedback(Model model) {
        feedbackReportService.findAll();
        model.addAttribute("feedbackReports", feedbackReportService.findAll());
        return "moderator/feedback";
    }
}
