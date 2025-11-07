
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

    @Autowired
    private AccountRepository accountRepo;

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

        // Nếu order này đã có feedback rồi -> quay về trang orders + thông báo
        if (feedbackRepo.existsByOrder(order)) {
            ra.addFlashAttribute("error",
                    "Với mỗi 1 đơn hàng bạn chỉ có thể add 1 feedback.");
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
            // Đúng lỗi "đã có feedback cho order này rồi" (409 CONFLICT)
            if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                ra.addFlashAttribute("error",
                        "Với mỗi 1 đơn hàng bạn chỉ có thể add 1 feedback.");
                return "redirect:/orders";
            }
            // Các lỗi khác giữ nguyên behavior cũ
            throw ex;
        }
    }

    // ================== EDIT ==================

    /** Form sửa feedback (GET) */
    @GetMapping("/feedback/{feedbackId}/edit")
    public String editFeedbackForm(@PathVariable Long feedbackId,
                                   Model model,
                                   Principal principal) {
        var view = feedbackService.getFeedbackForEdit(feedbackId, principal);
        model.addAttribute("tool", view.feedback().getTool());
        model.addAttribute("fb", view.feedback());
        model.addAttribute("fbComment", view.normalizedComment());
        return "customer/feedback-edit-form";
    }

    /** Update feedback (POST) */
    @PostMapping("/feedback/{feedbackId}/edit")
    public String updateFeedback(@PathVariable Long feedbackId,
                                 @RequestParam @Min(1) @Max(5) Integer rating,
                                 @RequestParam(required = false) @Size(max = 100) String comment,
                                 @RequestParam(required = false) Feedback.Status status,
                                 RedirectAttributes ra,
                                 Principal principal) {

        Long toolId = feedbackService.updateFeedback(feedbackId, rating, comment, status, principal);
        ra.addFlashAttribute("ok", "Đã cập nhật đánh giá.");
        return "redirect:/tools/" + toolId + "#review";
    }

    // ================== DELETE ==================

    /** “Xoá” feedback => đánh dấu SUSPECT (soft-delete) */
    @PostMapping("/feedback/{feedbackId}/delete")
    public String deleteFeedback(@PathVariable Long feedbackId,
                                 RedirectAttributes ra,
                                 Principal principal) {
        Long toolId = feedbackService.deleteFeedback(feedbackId, principal);
        ra.addFlashAttribute("ok", "Đã ẩn đánh giá."); // đổi thông điệp cho đúng hành vi soft-delete
        return "redirect:/tools/" + toolId + "#review";
    }
}
