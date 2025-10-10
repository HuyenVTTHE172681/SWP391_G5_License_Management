package swp391.fa25.lms.controller.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.FeedBackRepo;
import swp391.fa25.lms.repository.ToolRepo;

import java.util.Optional;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private final ToolRepo toolRepo;
    private final FeedBackRepo feedbackRepo;

    public FeedbackController(ToolRepo toolRepo, FeedBackRepo feedbackRepo) {
        this.toolRepo = toolRepo;
        this.feedbackRepo = feedbackRepo;
    }
    @GetMapping("/tool/{toolId}")
    public String viewToolFeedback(@PathVariable Long toolId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model, RedirectAttributes ra) {
        var toolOpt = toolRepo.findById(toolId);
        if (toolOpt.isEmpty()) {
            ra.addFlashAttribute("msg", "Tool không tồn tại");
            return "redirect:/";
        }
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 50);

        var tool = toolOpt.get();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Feedback> fbPage = feedbackRepo.findByTool(tool, pageable);

        double avg = Optional.ofNullable(feedbackRepo.averageRating(tool)).orElse(0.0);
        long total = feedbackRepo.countByTool(tool);

        model.addAttribute("tool", tool);
        model.addAttribute("fbPage", fbPage);
        model.addAttribute("avgRating", avg);
        model.addAttribute("totalReviews", total);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentPage", page);

        return "common/feedback";
    }
}

