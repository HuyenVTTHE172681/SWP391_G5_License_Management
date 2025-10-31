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
import swp391.fa25.lms.service.customer.CategoryService;
import swp391.fa25.lms.service.customer.FeedbackReadService;
import swp391.fa25.lms.service.customer.ToolService;

import java.util.Optional;

import java.util.List;
import java.util.stream.Collectors;


@Controller
public class ToolCommonController {
    @Autowired
    private ToolService toolService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private FeedbackReadService feedbackReadService;

    /**
     * Hiển thị trang detail tool
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

        // ===== THÊM: nạp repliesMap để hiển thị phản hồi seller ngay dưới feedback =====
        List<Long> fbIds = feedbackPage.getContent()
                .stream()
                .map(Feedback::getFeedbackId)
                .collect(Collectors.toList());
        var repliesMap = feedbackReadService.mapRepliesByFeedbackIds(fbIds);
        model.addAttribute("repliesMap", repliesMap);
        // ==============================================================================

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
