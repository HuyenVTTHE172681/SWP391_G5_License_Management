package swp391.fa25.lms.controller.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.FeedbackReport;
import swp391.fa25.lms.service.moderator.FeedbackReportService;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/moderator/feedback")
public class ModeratorFeedbackController {

    @Autowired
    @Qualifier("moderator")
    private FeedbackReportService feedbackReportService;

    // List + Filters
    @GetMapping({"", "/", "/list"})
    public String list(
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) FeedbackReport.Reason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) FeedbackReport.Status status,
            Model model
    ) {
        List<FeedbackReport> reports = feedbackReportService.findAll(reporterId, reason, fromDate, toDate, status);

        model.addAttribute("feedbackReports", reports);

        // keep filter values
        model.addAttribute("reporterId", reporterId);
        model.addAttribute("reason", reason);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("status", status);

        // enums for dropdowns
        model.addAttribute("reasons", FeedbackReport.Reason.values());
        model.addAttribute("statuses", FeedbackReport.Status.values());

        return "moderator/feedback";
    }

    // Approve
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        feedbackReportService.approve(id);
        ra.addFlashAttribute("msg", "Approved report #" + id);
        return "redirect:/moderator/feedback";
    }

    // Reject
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        feedbackReportService.reject(id);
        ra.addFlashAttribute("msg", "Rejected report #" + id);
        return "redirect:/moderator/feedback";
    }
}
