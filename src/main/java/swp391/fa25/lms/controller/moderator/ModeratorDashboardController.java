package swp391.fa25.lms.controller.moderator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolFile;
import swp391.fa25.lms.service.moderator.ToolFileService;
import swp391.fa25.lms.service.moderator.ToolService;
import swp391.fa25.lms.service.used.CategoryService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/moderator")
public class ModeratorDashboardController {

    @Autowired
    @Qualifier("moderatorToolService")
    private ToolService toolService;
    @Autowired
    @Qualifier("moderatorToolFileService")
    private ToolFileService toolFileService;
    @Autowired
    private CategoryService categoryService;

    // View tool uploaded
    @GetMapping("/history")
    public String displayHistoryUploadedRequest(
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

    //view request upload tool  pending
    @GetMapping("/uploadRequest")
    public String displayRequestForm(@RequestParam(required = false) Long sellerId,
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

    // ✅ 2️⃣ Xem chi tiết tool
    @GetMapping("/tool/{id}")
    public String viewToolDetail(@PathVariable("id") Long id, Model model) {
        Tool tool = toolService.findById(id);
        List<ToolFile> toolFiles = toolFileService.findByTool(tool);
        if (tool == null) {
            model.addAttribute("errorMessage", "Không tìm thấy tool.");
            return "redirect:/moderator/uploadRequest";
        }
        model.addAttribute("tool", tool);
        model.addAttribute("toolFiles", toolFiles);
        return "moderator/toolDetail";
    }

    // ✅ Approve tool
    @PostMapping("/tool/{id}/approve")
    public String approveTool(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Tool tool = toolService.findById(id);
        if (tool == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Không tìm thấy tool để duyệt.");
            return "redirect:/moderator/uploadRequest";
        }
        tool.setStatus(Tool.Status.APPROVED);
        tool.setNote(null);
        tool.setUpdatedAt(LocalDateTime.now());
        toolService.save(tool);

        redirectAttributes.addFlashAttribute("successMessage", "✅ Tool đã được duyệt thành công!");
        return "redirect:/moderator/uploadRequest";
    }

    // ✅ Reject tool
    @PostMapping("/tool/{id}/reject")
    public String rejectTool(@PathVariable("id") Long id,
                             @RequestParam("reason") String reason,
                             RedirectAttributes redirectAttributes) {
        Tool tool = toolService.findById(id);
        if (tool == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Không tìm thấy tool để từ chối.");
            return "redirect:/moderator/uploadRequest";
        }

        tool.setStatus(Tool.Status.REJECTED);
        tool.setNote(reason);
        tool.setUpdatedAt(LocalDateTime.now());
        toolService.save(tool);

        redirectAttributes.addFlashAttribute("errorMessage", "❌ Tool đã bị từ chối với lý do: " + reason);
        return "redirect:/moderator/uploadRequest";
    }

}
