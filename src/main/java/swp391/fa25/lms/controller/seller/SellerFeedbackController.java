package swp391.fa25.lms.controller.seller;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReply;
import swp391.fa25.lms.repository.FeedBackReplyRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.service.customer.ToolService;

import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/seller/feedback")
public class SellerFeedbackController {

    private final FeedbackRepository feedbackRepo;
    private final FeedBackReplyRepository replyRepo;
    private final ToolService toolService;

    public SellerFeedbackController(FeedbackRepository feedbackRepo,
                                    FeedBackReplyRepository replyRepo,
                                    ToolService toolService) {
        this.feedbackRepo = feedbackRepo;
        this.replyRepo = replyRepo;
        this.toolService = toolService;
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
                .filter(f -> f.getTool().getSeller() != null
                        && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                .toList()
                : feedbackRepo.findByTool(toolService.findById(toolId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))).stream()
                .filter(f -> f.getTool().getSeller() != null
                        && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                .toList();

        model.addAttribute("feedbacks", list);
        model.addAttribute("toolId", toolId);
        return "seller/feedback-list";
    }

    @PostMapping("/{feedbackId}/reply")
    @Transactional
    public String upsertReply(@PathVariable Long feedbackId,
                              @RequestParam @Size(max=500) String content,
                              Principal principal) {

        Account seller = getCurrentSeller(principal);
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        // chỉ seller của tool đó mới được reply
        if (fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
        }

        var existing = replyRepo.findByFeedback_FeedbackId(feedbackId).orElse(null);
        if (existing == null) {
            var rp = new FeedbackReply();
            rp.setFeedback(fb);
            rp.setSeller(seller);
            rp.setContent(content.trim());
            rp.setCreatedAt(LocalDateTime.now());
            rp.setUpdatedAt(LocalDateTime.now());
            replyRepo.save(rp);
        } else {
            existing.setContent(content.trim());
            existing.setUpdatedAt(LocalDateTime.now());
            replyRepo.save(existing);
        }

        return "redirect:/seller/feedback";
    }

    @PostMapping("/{feedbackId}/reply/delete")
    @Transactional
    public String deleteReply(@PathVariable Long feedbackId, Principal principal) {
        Account seller = getCurrentSeller(principal);
        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        replyRepo.deleteByFeedback_FeedbackId(feedbackId);
        return "redirect:/seller/feedback";
    }

    private Account getCurrentSeller(Principal principal) {
        Account acc = new Account();
        acc.setAccountId(1L); // TODO: thay bằng map từ principal
        return acc;
    }
}
