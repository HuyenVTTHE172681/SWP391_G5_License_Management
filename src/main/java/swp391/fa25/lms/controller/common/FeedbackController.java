package swp391.fa25.lms.controller.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.service.customer.FeedbackRepositoryImpl;

import java.security.Principal;

@Controller
@Validated
public class FeedbackController {

    @Autowired
    @Qualifier("customerFeedBack")
    private FeedbackRepositoryImpl feedbackService;

    @Autowired
    private FeedbackRepository feedbackRepo;
    public FeedbackController(FeedbackRepositoryImpl feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ================== CREATE (theo đơn hàng) ==================

    /** Hiển thị form feedback cho 1 đơn */
    @GetMapping("/orders/{orderId}/feedback")
    public String showFeedbackForm(@PathVariable Long orderId,
                                   Model model,
                                   RedirectAttributes ra) {

        var order = feedbackService.getOrderForFeedback(orderId);

        Long accId  = order.getAccount().getAccountId();
        Long toolId = order.getTool().getToolId();

        // Nếu account + tool này đã có feedback rồi -> quay về trang orders + thông báo
        if (feedbackRepo.existsByAccount_AccountIdAndTool_ToolId(accId, toolId)) {
            ra.addFlashAttribute("error",
                    "Với mỗi 1 đơn hàng / 1 sản phẩm bạn chỉ có thể đánh giá 1 lần.");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("tool", order.getTool());
        return "customer/feedback-form";
    }

    /** Tạo feedback mới */
    @PostMapping("/orders/{orderId}/feedback")
    public String submitFeedback(@PathVariable Long orderId,
                                 @RequestParam @Min(1) @Max(5) Integer rating,
                                 @RequestParam(required = false) @Size(max = 100) String comment,
                                 RedirectAttributes ra) {
        try {
            Long toolId = feedbackService.submitFeedback(orderId, rating, comment);
            ra.addFlashAttribute("ok", "Cảm ơn bạn! Đánh giá đã được ghi nhận.");
            return "redirect:/tools/" + toolId + "#review";

        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                ra.addFlashAttribute("error",
                        "Với mỗi 1 đơn hàng / 1 sản phẩm bạn chỉ có thể đánh giá 1 lần.");
                return "redirect:/orders";
            }
            throw ex;
        }
    }

    // ================== EDIT ==================

    /** Form sửa feedback (GET) */
    @GetMapping("/feedback/{feedbackId}/edit")
    public String editFeedbackForm(@PathVariable Long feedbackId,
                                   Model model,
                                   Principal principal,
                                   RedirectAttributes ra) {
        try {
            var view = feedbackService.getFeedbackForEdit(feedbackId, principal);

            model.addAttribute("tool", view.feedback().getTool());
            model.addAttribute("fb", view.feedback());
            model.addAttribute("fbComment", view.normalizedComment());
            return "customer/feedback-edit-form";

        } catch (ResponseStatusException ex) {
            // Chỉ xử lý case FORBIDDEN (hết thời gian/không có quyền)
            if (ex.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {

                Long toolId = feedbackService.getToolIdForFeedback(feedbackId);

                ra.addFlashAttribute("error",
                        ex.getReason() != null
                                ? ex.getReason()
                                : "Bạn không thể chỉnh sửa đánh giá này.");

                if (toolId != null) {
                    return "redirect:/tools/" + toolId + "#review";
                } else {
                    return "redirect:/tools";
                }
            }

            // Các lỗi khác để Spring xử lý như cũ
            throw ex;
        }
    }

    /** Update feedback (POST) */
    @PostMapping("/feedback/{feedbackId}/edit")
    public String updateFeedback(@PathVariable Long feedbackId,
                                 @RequestParam @Min(1) @Max(5) Integer rating,
                                 @RequestParam(required = false) @Size(max = 100) String comment,
                                 @RequestParam(required = false) Feedback.Status status,
                                 RedirectAttributes ra,
                                 Principal principal) {

        try {
            Long toolId = feedbackService.updateFeedback(feedbackId, rating, comment, status, principal);
            ra.addFlashAttribute("ok", "Đã cập nhật đánh giá.");
            return "redirect:/tools/" + toolId + "#review";

        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                Long toolId = feedbackService.getToolIdForFeedback(feedbackId);

                ra.addFlashAttribute("error",
                        ex.getReason() != null
                                ? ex.getReason()
                                : "Bạn không thể chỉnh sửa đánh giá này.");

                if (toolId != null) {
                    return "redirect:/tools/" + toolId + "#review";
                } else {
                    return "redirect:/tools";
                }
            }
            throw ex;
        }
    }


//    // ================== DELETE ==================
//
//    /** “Xoá” feedback => đánh dấu SUSPECT (soft-delete) */
//    @PostMapping("/feedback/{feedbackId}/delete")
//    public String deleteFeedback(@PathVariable Long feedbackId,
//                                 RedirectAttributes ra,
//                                 Principal principal) {
//        Long toolId = feedbackService.deleteFeedback(feedbackId, principal);
//        ra.addFlashAttribute("ok", "Đã ẩn đánh giá.");
//        return "redirect:/tools/" + toolId + "#review";
//    }
}
