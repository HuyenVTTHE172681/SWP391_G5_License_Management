package swp391.fa25.lms.controller.moderator;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.service.moderator.ToolFileService;
import swp391.fa25.lms.service.moderator.ToolReportService;
import swp391.fa25.lms.service.moderator.ToolService;
import swp391.fa25.lms.service.used.CategoryService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/moderator")
public class ModeratorToolController {

    @Autowired
    @Qualifier("moderatorToolReportService")
    private ToolReportService toolReportService;
    @Autowired
    @Qualifier("moderatorToolService")
    private ToolService toolService;
    @Autowired
    @Qualifier("moderatorToolFileService")
    private ToolFileService toolFileService;
    @Autowired
    private CategoryService categoryService;

    @GetMapping({"", "/dashboard"})
    public String moderatorDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "moderator/dashboard";
    }
    // View tool uploaded
    @GetMapping("/history")
    public String displayUploadedRequest(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedTo,
            Model model) {

        List<Tool> toolList = toolService.filterNonPendingTools(
                sellerId,
                categoryId,
                uploadFrom,
                uploadTo,
                approvedFrom,
                approvedTo,
                "MOD",
                status
        );

        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("toolList", toolList);
        model.addAttribute("categories", categories);
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("uploadFrom", uploadFrom);
        model.addAttribute("uploadTo", uploadTo);
        model.addAttribute("approvedFrom", approvedFrom);
        model.addAttribute("approvedTo", approvedTo);

        return "moderator/uploaded";
    }

    //View request upload tool  pending
    @GetMapping("/uploadRequest")
    public String displayUploadRequest(@RequestParam(required = false) Long sellerId,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadFrom,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadTo,
                                     Model model) {
        List<Tool> toolList = toolService.filterPendingTools(sellerId, categoryId, uploadFrom, uploadTo);
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("toolList", toolList);
        model.addAttribute("categories", categories);
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("uploadFrom", uploadFrom);
        model.addAttribute("uploadTo", uploadTo);
        return "moderator/request";
    }

    // View tool detail
    @GetMapping("/tool/{id}")
    public String viewToolDetail(@PathVariable("id") Long id, Model model) {
        Tool tool = toolService.findById(id);
        if (tool == null) {
            model.addAttribute("errorMessage", "Tool not found");
            return "redirect:/moderator/uploadRequest";
        }
        model.addAttribute("tool", tool);
        return "moderator/toolDetail";
    }

    //  Approve tool
    @PostMapping("/tool/{id}/approve")
    public String approveTool(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        Tool tool = toolService.findById(id);
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");

        if (tool == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tool not found");
            return "redirect:/moderator/uploadRequest";
        }
        toolService.approveTool(tool, account.getRole().getRoleName().toString());

        redirectAttributes.addFlashAttribute("successMessage", "Approved");
        return "redirect:/moderator/uploadRequest";
    }

    //  Reject tool
    @PostMapping("/tool/{id}/reject")
    public String rejectTool(@PathVariable("id") Long id,
                             @RequestParam("reason") String reason,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        Tool tool = toolService.findById(id);
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (tool == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tool not found.");
            return "redirect:/moderator/uploadRequest";
        }

        toolService.rejectTool(tool, reason, account.getRole().getRoleName().toString());

        redirectAttributes.addFlashAttribute("errorMessage", "Rejected because" + reason);
        return "redirect:/moderator/uploadRequest";
    }

    @GetMapping("/tool/report")
    public String viewToolReports(
            @RequestParam(required = false) ToolReport.Status status,
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate toDate,
            Model model) {

        List<ToolReport> reports = toolReportService.filterReports(status, toolId, reporterId, fromDate, toDate);

        model.addAttribute("reports", reports);
        model.addAttribute("status", status);
        model.addAttribute("toolId", toolId);
        model.addAttribute("reporterId", reporterId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "moderator/tool-report";
    }
    @PostMapping("/tool/report/{id}/approve")
    public String approveReport(@PathVariable Long id, RedirectAttributes redirect) {
        toolReportService.updateStatus(id, ToolReport.Status.APPROVED);
        redirect.addFlashAttribute("success", "✅ Report approved successfully!");
        return "redirect:/moderator/tool/report";
    }
    @PostMapping("/tool/report/{id}/reject")
    public String rejectReport(@PathVariable Long id, RedirectAttributes redirect) {
        toolReportService.updateStatus(id, ToolReport.Status.REJECTED);
        redirect.addFlashAttribute("error", "Report rejected.");
        return "redirect:/moderator/tool/report";
    }
}
