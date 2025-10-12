package swp391.fa25.lms.controller.moderator;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.ToolRepository;
import swp391.fa25.lms.service.used.CategoryService;
import swp391.fa25.lms.service.used.ToolService;

import java.util.List;

@Controller
@RequestMapping("/moderator")
public class ModeratorDashboardController {
    @Autowired
    ToolRepository toolRepository;
    @Autowired
    ToolService toolService;
    @Autowired
    CategoryService categoryService;

    @GetMapping("/upload-request")
    public String displayUploadRequest(
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            Model model) {

        // 🔹 Lấy danh sách tool theo filter
        List<Tool> toolList = toolService.filterToolsForModerator(toolName, categoryId, status);
        List<Category> categories = categoryService.getCategories();

        model.addAttribute("toolList", toolList);
        model.addAttribute("categories", categories);
        model.addAttribute("toolName", toolName);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);

        return "moderator/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approveTool(@PathVariable("id") Long id) {
        Tool tool = toolService.findById(id);
        if (tool != null) {
            tool.setStatus(Tool.Status.APPROVED);
            toolService.save(tool);
        }
        return "redirect:/moderator/upload-request";
    }

    @PostMapping("/reject/{id}")
    public String rejectTool(@PathVariable("id") Long id) {
        Tool tool = toolService.findById(id);
        if (tool != null) {
            tool.setStatus(Tool.Status.REJECTED);
            toolService.save(tool);
        }
        return "redirect:/moderator/upload-request";
    }
}
