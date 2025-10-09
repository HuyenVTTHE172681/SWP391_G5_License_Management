package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.CategoryService;
import swp391.fa25.lms.service.FeedbackService;
import swp391.fa25.lms.service.ToolService;

import java.util.List;

@Controller
@RequestMapping("/homepage")
public class HomePageController {

    @Autowired
    private ToolService toolService;

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public String displayToolList(Model model) {
        try {
            List<Category> categories = categoryService.getCategories();
            List<Tool> tools = toolService.availableTools(Tool.Status.APPROVED);

            model.addAttribute("tools", tools);
            model.addAttribute("categories", categories);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "public/homepage"; // ✅ Tên view (trong templates/public/homepage.html)
    }

    @GetMapping("/filter")
    public String filterTools(
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String priceOrder,
            @RequestParam(required = false) String updateDate,
            Model model) {

        List<Tool> tools = toolService.filterTools(
                toolName, categoryId,
                "price", priceOrder,
                "updatedAt", updateDate
        );

        model.addAttribute("tools", tools);
        model.addAttribute("categories", categoryService.getCategories());
        model.addAttribute("priceOrder", priceOrder);
        model.addAttribute("updateDate", updateDate);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("toolName", toolName);
        return "public/homepage";
    }
    @GetMapping("/toolDetail/{id}")
    public String toolDetail(
            @PathVariable("id") long id,
            Model model
    ) {
        try {
            Tool tool = toolService.findById(id);
            List<Feedback> feedbacks = feedbackService.findByTool(tool);
            if (tool == null) {
                // Nếu không tìm thấy tool, có thể redirect hoặc báo lỗi
                model.addAttribute("errorMessage", "Tool not found!");
                return "common/error";
            } else {
                model.addAttribute("tool", tool);
                model.addAttribute("feedbacks", feedbacks);
                return "public/toolDetail";

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "common/error";
    }


}
