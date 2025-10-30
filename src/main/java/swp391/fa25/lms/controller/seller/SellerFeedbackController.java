package swp391.fa25.lms.controller.seller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReply;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FeedBackReplyRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.service.customer.ToolService;

import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/seller/feedback")
public class SellerFeedbackController {
    private static boolean isSpacedOutText(String s) {
        if (s == null) return false;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() < 9) return false;
        String[] parts = t.split(" ");
        if (parts.length < 5) return false;
        for (String p : parts) {
            if (!p.matches("[\\p{L}\\p{N}]")) return false;
        }
        return true;
    }

    public record ReplyVM(Long feedbackId, Long replyId, String sellerName, String content, String updatedAt) {}

    private final FeedbackRepository feedbackRepo;
    private final FeedBackReplyRepository replyRepo;
    private final ToolService toolService;
    private final AccountRepository accountRepo;

    public SellerFeedbackController(FeedbackRepository feedbackRepo,
                                    FeedBackReplyRepository replyRepo,
                                    ToolService toolService,
                                    AccountRepository accountRepo) {
        this.feedbackRepo = feedbackRepo;
        this.replyRepo = replyRepo;
        this.toolService = toolService;
        this.accountRepo = accountRepo;
    }

    // Danh sách feedback của các tool mà seller đang bán
    @GetMapping
    @Transactional(readOnly = true)

    public String listFeedbacks(@RequestParam(required = false) Long toolId,
                                Model model,
                                Principal principal) {

        Account seller = getCurrentSeller(principal);

        List<Feedback> list = (toolId == null)
                ? feedbackRepo.findAll().stream()
                .filter(f -> f.getTool() != null
                        && f.getTool().getSeller() != null
                        && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                .toList()
                : feedbackRepo.findByTool(
                        toolService.findById(toolId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                ).stream()
                .filter(f -> f.getTool() != null
                        && f.getTool().getSeller() != null
                        && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                .toList();

        model.addAttribute("feedbacks", list);
        model.addAttribute("toolId", toolId);
        return "seller/feedback-list";
    }

    @PostMapping("/{feedbackId}/reply")
    @Transactional
    public Object upsertReply(@PathVariable Long feedbackId,
                              @RequestParam @Size(max = 200) String content,
                              @RequestParam(required = false) Long toolId,
                              @RequestHeader(value = "X-Requested-With", required = false) String xrw,
                              @RequestHeader(value = "Accept", required = false) String accept,
                              Principal principal) {

        Account seller = getCurrentSeller(principal);
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        if (fb.getTool() == null || fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
        }

        String body = content == null ? "" : content.trim();
        if (body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung phản hồi không được rỗng.");
        }
        if (isSpacedOutText(body)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung phản hồi không hợp lệ: không chèn khoảng trắng giữa từng ký tự.");
        }

        var existing = replyRepo.findByFeedback_FeedbackId(feedbackId).orElse(null);
        FeedbackReply saved;
        if (existing == null) {
            var rp = new FeedbackReply();
            rp.setFeedback(fb);
            rp.setSeller(seller);
            rp.setContent(body);
            rp.setCreatedAt(LocalDateTime.now());
            rp.setUpdatedAt(LocalDateTime.now());
            saved = replyRepo.save(rp);
        } else {
            existing.setContent(body);
            existing.setUpdatedAt(LocalDateTime.now());
            saved = replyRepo.save(existing);
        }

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        ReplyVM vm = new ReplyVM(
                fb.getFeedbackId(),
                saved.getReplyId(),
                seller.getFullName() != null ? seller.getFullName() : "Seller",
                saved.getContent(),
                saved.getUpdatedAt() != null ? saved.getUpdatedAt().format(fmt) : ""
        );

        if (isAjax) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(vm);
        }
        return "redirect:/seller/feedback" + (toolId != null ? ("?toolId=" + toolId) : "");
    }
    @PostMapping("/{feedbackId}/reply/delete")
    @Transactional
    public Object deleteReply(@PathVariable Long feedbackId,
                              @RequestParam(required = false) Long toolId, // nhận toolId thay vì redirect string
                              @RequestHeader(value = "X-Requested-With", required = false) String xrw,
                              Principal principal) {
        Account seller = getCurrentSeller(principal);
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (fb.getTool() == null
                || fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        replyRepo.deleteByFeedback_FeedbackId(feedbackId);

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xrw);
        if (isAjax) {
            // ở lại trang hiện tại, front-end tự xoá DOM, không redirect
            return ResponseEntity.ok().build();
        }

        // Non-AJAX: reload lại trang danh sách của seller (giữ filter nếu có)
        return "redirect:/seller/feedback" + (toolId != null ? ("?toolId=" + toolId) : "");
    }

    private Account getCurrentSeller(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        String email = principal.getName(); // username = email
        Long accId = accountRepo.findIdByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
        // Lấy đầy đủ Account để dùng fullName khi trả JSON
        return accountRepo.findById(accId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
    }
}
