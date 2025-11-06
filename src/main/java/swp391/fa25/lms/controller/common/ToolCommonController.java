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
     * Hiển thị trang detail tool (chỉ cho Tool ở trạng thái PUBLISHED).
     * Feedback cũng chỉ lấy PUBLISHED (hoặc NULL nếu DB cũ) theo logic trong Service.
     */
    @GetMapping("/tools/{id}")
    public String showToolDetail(@PathVariable("id") Long id,
                                 @RequestParam(value = "reviewPage", defaultValue = "0") int reviewPage,
                                 Model model) {

        // Lấy tool theo id, CHỈ trạng thái PUBLISHED
        Optional<Tool> maybeTool = toolService.findPublishedToolById(id);
        if (maybeTool.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy sản phẩm hoặc sản phẩm chưa công khai.");
            return "public/404";
        }

        Tool tool = maybeTool.get();

        // Guard trang âm
        int page = Math.max(0, reviewPage);

        // Lấy danh sách feedback CHỈ PUBLISHED (theo Cách B: Sort nằm ở Pageable trong Service)
        Page<Feedback> feedbackPage = toolService.getFeedbackPageForTool(
                tool, page, 5, Feedback.Status.PUBLISHED
        );

        // Nạp repliesMap để hiển thị phản hồi seller dưới từng feedback
        List<Long> fbIds = feedbackPage.getContent()
                .stream()
                .map(Feedback::getFeedbackId)
                .collect(Collectors.toList());
        var repliesMap = feedbackReadService.mapRepliesByFeedbackIds(fbIds);
        model.addAttribute("repliesMap", repliesMap);
        // ==============================================================================

        // Tính rating trung bình
        double avgRating = toolService.getAverageRatingForTool(tool);
        System.out.println("");
        // Tổng số review
        long totalReviews = toolService.getTotalReviewsForTool(tool);
        // Tính rating trung bình & tổng review (chỉ PUBLISHED)
        double avgRating = toolService.getAverageRatingForTool(tool, Feedback.Status.PUBLISHED);
        long totalReviews = toolService.getTotalReviewsForTool(tool, Feedback.Status.PUBLISHED);

        // Data cho view
        model.addAttribute("tool", tool);
        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("categories", categoryService.getAllCategories());

        // Tham số để view biết đang lọc theo status nào (nếu cần hiển thị)
        model.addAttribute("feedbackStatus", Feedback.Status.PUBLISHED.name());

        return "public/tool-detail";
    }
}
