package swp391.fa25.lms.controller.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.moderator.CategoryService;
import swp391.fa25.lms.service.manager.ToolService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    @Autowired
    @Qualifier("manageToolService")
    private ToolService toolService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping({"", "/"})
    public String managerDashboard(Model model) {
        return "manager/dashboard";
    }

    @GetMapping("/upload-tool")
    public String upload(@RequestParam(required = false) Long sellerId,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadFrom,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadTo,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedFrom,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedTo,
                         Model model) {

        List<Tool> toolList = toolService.filterApprovedTools(
                sellerId,
                categoryId,
                uploadFrom,
                uploadTo,
                approvedFrom,
                approvedTo
        );

        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("toolList", toolList);
        model.addAttribute("categories", categories);
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("uploadFrom", uploadFrom);
        model.addAttribute("uploadTo", uploadTo);
        model.addAttribute("approvedFrom", approvedFrom);
        model.addAttribute("approvedTo", approvedTo);

        return "manager/upload-tool";
    }

    @GetMapping("/tool/{id}")
    public String viewToolDetail(@PathVariable Long id, Model model) {
        Tool tool = toolService.findById(id);
        if (tool == null) {
            model.addAttribute("error", "Tool not found.");
            return "redirect:/manager/upload-tool";
        }

        model.addAttribute("tool", tool);
        return "manager/tool-detail";
    }

    @PostMapping("/tool/{id}/publish")
    public String publishTool(@PathVariable Long id, RedirectAttributes redirect, HttpServletRequest request) {
        Tool tool = toolService.findById(id);
        if (tool != null) {
            tool.setStatus(Tool.Status.PUBLISHED);
            tool.setReviewedBy(request.getSession().getAttribute("loggedInAccount").toString());
            tool.setUpdatedAt(LocalDateTime.now());
            toolService.save(tool);
            redirect.addFlashAttribute("success", "Tool has been published successfully!");
        }
        return "redirect:/manager/tool/" + id;
    }

    @PostMapping("/tool/{id}/pending")
    public String pendingTool(@PathVariable Long id,
                              @RequestParam("note") String note,
                              RedirectAttributes redirect,
                              HttpServletRequest request) {
        Tool tool = toolService.findById(id);
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (tool != null) {
            tool.setStatus(Tool.Status.PENDING);
            tool.setNote(note);
            tool.setReviewedBy(account.getRole().getRoleName().toString());
            tool.setUpdatedAt(LocalDateTime.now());
            toolService.save(tool);
            redirect.addFlashAttribute("info", "Tool has been set to Pending.");
        }
        return "redirect:/manager/tool/" + id;
    }

    @PostMapping("/tool/{id}/reject")
    public String rejectTool(@PathVariable Long id,
                             @RequestParam("note") String note,
                             RedirectAttributes redirect,
                             HttpServletRequest request) {
        Tool tool = toolService.findById(id);
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (tool != null) {
            tool.setStatus(Tool.Status.REJECTED);
            tool.setNote(note);
            tool.setReviewedBy(account.getRole().getRoleName().toString());
            tool.setUpdatedAt(LocalDateTime.now());
            toolService.save(tool);
            redirect.addFlashAttribute("error", "Tool has been rejected.");
        }
        return "redirect:/manager/tool/" + id;
    }

    @GetMapping("/tool/list")
    public String listTools(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedTo,
            @RequestParam(required = false) String reviewedBy,
            @RequestParam(required = false) String status,
            Model model
    ) {
        List<Tool> tools = toolService.filterNonPendingTools(
                sellerId, categoryId, uploadFrom, uploadTo, approvedFrom, approvedTo, reviewedBy, status
        );
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("tools", tools);
        model.addAttribute("selectedSellerId", sellerId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("uploadFrom", uploadFrom);
        model.addAttribute("uploadTo", uploadTo);
        model.addAttribute("approvedFrom", approvedFrom);
        model.addAttribute("approvedTo", approvedTo);
        model.addAttribute("reviewedBy", reviewedBy);
        model.addAttribute("status", status);

        return "manager/tool-list";
    }


}
