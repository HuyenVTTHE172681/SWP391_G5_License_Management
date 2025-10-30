// src/main/java/swp391/fa25/lms/controller/common/FeedbackController.java
package swp391.fa25.lms.controller.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.repository.*;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@Validated
public class FeedbackController {

    private final CustomerOrderRepository orderRepo;
    private final FeedbackRepository feedbackRepo;
    private final FeedBackReplyRepository feedbackReplyRepo;
    private final AccountRepository accountRepo;

    public FeedbackController(CustomerOrderRepository orderRepo,
                              FeedbackRepository feedbackRepo,
                              FeedBackReplyRepository feedbackReplyRepo,
                              AccountRepository accountRepo) {
        this.orderRepo = orderRepo;
        this.feedbackRepo = feedbackRepo;
        this.feedbackReplyRepo = feedbackReplyRepo;
        this.accountRepo = accountRepo;
    }

    private CustomerOrder loadOrderOr404(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    /** Lấy accountId hiện tại từ Principal (email) */
    private Long getCurrentAccountId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        String email = principal.getName(); // Security đang dùng email làm username
        return accountRepo.findIdByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
    }

    // ================== CREATE (theo đơn hàng) ==================

    /** Hiển thị form feedback cho 1 đơn */
    @GetMapping("/orders/{orderId}/feedback")
    @Transactional(readOnly = true)
    public String showFeedbackForm(@PathVariable Long orderId, Model model) {
        var order = loadOrderOr404(orderId);
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn thành công mới được đánh giá.");
        }
        model.addAttribute("order", order);
        model.addAttribute("tool", order.getTool());
        return "customer/feedback-form";
    }

    /** Tạo feedback mới (một người mua có thể feedback nhiều lần) */
    @PostMapping("/orders/{orderId}/feedback")
    @Transactional
    public String submitFeedback(@PathVariable Long orderId,
                                 @RequestParam @Min(1) @Max(5) Integer rating,
                                 @RequestParam(required = false) @Size(max = 100) String comment,
                                 RedirectAttributes ra) {
        var order = loadOrderOr404(orderId);
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn thành công mới được đánh giá.");
        }

        var fb = new Feedback();
        fb.setAccount(order.getAccount());
        fb.setTool(order.getTool());
        fb.setRating(rating);
        fb.setComment(comment == null ? "" : comment.trim());
        fb.setCreatedAt(LocalDateTime.now());

        feedbackRepo.save(fb);

        ra.addFlashAttribute("ok", "Cảm ơn bạn! Đánh giá đã được ghi nhận.");
        return "redirect:/tools/" + order.getTool().getToolId() + "#review";
    }

    // ================== EDIT/DELETE (không cần orderId) ==================

    /** Form sửa feedback của chính chủ */
    @GetMapping("/feedback/{feedbackId}/edit")
    @Transactional(readOnly = true)
    public String editFeedbackForm(@PathVariable Long feedbackId,
                                   Model model, Principal principal) {
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        Long currentAccId = getCurrentAccountId(principal);
        if (!fb.getAccount().getAccountId().equals(currentAccId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
        }

        model.addAttribute("tool", fb.getTool());
        model.addAttribute("fb", fb);
        return "customer/feedback-edit-form";
    }

    /** Cập nhật feedback của chính chủ */
    @PostMapping("/feedback/{feedbackId}/edit")
    @Transactional
    public String updateFeedback(@PathVariable Long feedbackId,
                                 @RequestParam @Min(1) @Max(5) Integer rating,
                                 @RequestParam(required = false) @Size(max = 100) String comment,
                                 RedirectAttributes ra,
                                 Principal principal) {
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        Long currentAccId = getCurrentAccountId(principal);
        if (!fb.getAccount().getAccountId().equals(currentAccId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
        }

        fb.setRating(rating);
        fb.setComment(comment == null ? "" : comment.trim());
        feedbackRepo.save(fb);

        ra.addFlashAttribute("ok", "Đã cập nhật đánh giá.");
        return "redirect:/tools/" + fb.getTool().getToolId() + "#review";
    }

    /** Xoá feedback của chính chủ (xoá kèm reply để tránh FK) */
    @PostMapping("/feedback/{feedbackId}/delete")
    @Transactional
    public String deleteFeedback(@PathVariable Long feedbackId,
                                 RedirectAttributes ra,
                                 Principal principal) {
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        Long currentAccId = getCurrentAccountId(principal);
        if (!fb.getAccount().getAccountId().equals(currentAccId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
        }

        Long toolId = fb.getTool().getToolId();
        feedbackReplyRepo.deleteByFeedback_FeedbackId(fb.getFeedbackId()); // xoá reply trước
        feedbackRepo.delete(fb);

        ra.addFlashAttribute("ok", "Đã xoá đánh giá.");
        return "redirect:/tools/" + toolId + "#review";
    }
}
