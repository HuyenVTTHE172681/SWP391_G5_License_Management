package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.used.CategoryService;
import swp391.fa25.lms.service.used.ToolService;

import java.util.Optional;

@Controller
public class ToolCommonController {
    @Autowired
    private ToolService toolService;

    @Autowired
    private CategoryService categoryService;

    /**
     * Hiển thị trang detail tool
     * @param id
     * @param reviewPage
     * @param model
     * @return
     */
    @GetMapping("/tools/{id}")
    public String showToolDetail(@PathVariable("id") Long id,
                                 @RequestParam(value = "reviewPage", defaultValue = "0") int reviewPage,
                                 Model model) {

        // Lấy tool theo id
        Optional<Tool> maybeTool = toolService.findPublishedToolById(id);
        if (maybeTool.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy sản phẩm hoặc sản phẩm chưa công khai.");
            return "public/404"; // 404.html
        }

        Tool tool = maybeTool.get();

        // Lấy danh sách feedback
        Page<Feedback> feedbackPage = toolService.getFeedbackPageForTool(tool, reviewPage, 5);
        // Tính rating trung bình
        double avgRating = toolService.getAverageRatingForTool(tool);
        // Tổng số review
        long totalReviews = toolService.getTotalReviewsForTool(tool);

        // Data view
        model.addAttribute("tool", tool);
        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "public/tool-detail";
    }
}
