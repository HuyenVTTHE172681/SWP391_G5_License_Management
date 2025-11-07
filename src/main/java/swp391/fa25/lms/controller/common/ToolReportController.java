package swp391.fa25.lms.controller.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.service.customer.ToolReportService;

@Controller
@RequestMapping("/tools")
public class ToolReportController {
    @Autowired
    private ToolReportService toolReportService;


    @PostMapping("/{toolId}/report")
    public ResponseEntity<String> reportTool(
            @PathVariable Long toolId,
            @RequestParam("reason") ToolReport.Reason reason,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session) {

        Account reporter = (Account) session.getAttribute("loggedInAccount");
        if (reporter == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Vui lòng đăng nhập để báo cáo!");
        }

        String message = toolReportService.reportTool(reporter, toolId, reason, description);
        if (message.contains("trước đó")) {
            // đã báo cáo rồi
            return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
        }

        return ResponseEntity.ok(message);
    }

}
