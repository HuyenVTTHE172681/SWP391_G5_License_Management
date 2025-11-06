package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReport;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.service.customer.FeedbackReportService;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class FeedbackReportController {
    @Autowired
    private FeedbackReportService feedbackReportService;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private FeedbackRepository feedbackRepo;

    // ================== REPORT ==================
    @PostMapping("/feedback/{fid}/report")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> reportFeedback(
            @PathVariable Long fid,
            @RequestParam("reason") FeedbackReport.Reason reason,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session
    ) {
        Account reporter = (Account) session.getAttribute("loggedInAccount");
        if (reporter == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Vui lòng đăng nhập để báo cáo!");
        }

        String message = feedbackReportService.reportFeedback(fid, reporter, reason, description);

        // Trả mã lỗi tùy theo nội dung
        if (message.contains("không thể báo cáo")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
        } else if (message.contains("tối đa 2 feedback")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(message);
        } else if (message.contains("không tồn tại")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }

        return ResponseEntity.ok(message);
    }
}
