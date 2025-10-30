package swp391.fa25.lms.service.seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service("sellerFeedbackService")
public class SellerFeedbackService {

    public record ReplyVM(Long feedbackId, Long replyId, String sellerName, String content, String updatedAt) {}
    @Autowired
    private  FeedbackRepository feedbackRepo;
    @Autowired
    private  FeedBackReplyRepository replyRepo;
    @Autowired
    private  ToolService toolService;
    @Autowired
    private  AccountRepository accountRepo;

    public SellerFeedbackService(FeedbackRepository feedbackRepo,
                                 FeedBackReplyRepository replyRepo,
                                 ToolService toolService,
                                 AccountRepository accountRepo) {
        this.feedbackRepo = feedbackRepo;
        this.replyRepo = replyRepo;
        this.toolService = toolService;
        this.accountRepo = accountRepo;
    }

    // ================= LIST =================

    @Transactional(readOnly = true)
    public List<Feedback> listFeedbacks(Long toolId, Principal principal) {
        Account seller = getCurrentSeller(principal);

        if (toolId == null) {
            // Không tạo repo method mới: lọc bằng stream theo seller
            return feedbackRepo.findAll().stream()
                    .filter(f -> f.getTool() != null
                            && f.getTool().getSeller() != null
                            && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                    .toList();
        }

        var tool = toolService.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return feedbackRepo.findByTool(tool).stream()
                .filter(f -> f.getTool() != null
                        && f.getTool().getSeller() != null
                        && f.getTool().getSeller().getAccountId().equals(seller.getAccountId()))
                .toList();
    }

    // ================= UPSERT REPLY =================

    @Transactional
    public ReplyVM upsertReply(Long feedbackId,
                               @Size(max = 200) String content,
                               Principal principal) {
        Account seller = getCurrentSeller(principal);

        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback"));

        if (fb.getTool() == null
                || fb.getTool().getSeller() == null
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

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return new ReplyVM(
                fb.getFeedbackId(),
                saved.getReplyId(),
                seller.getFullName() != null ? seller.getFullName() : "Seller",
                saved.getContent(),
                saved.getUpdatedAt() != null ? saved.getUpdatedAt().format(fmt) : ""
        );
    }

    // ================= DELETE REPLY =================

    @Transactional
    public void deleteReply(Long feedbackId, Principal principal) {
        Account seller = getCurrentSeller(principal);

        var fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (fb.getTool() == null
                || fb.getTool().getSeller() == null
                || !fb.getTool().getSeller().getAccountId().equals(seller.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        replyRepo.deleteByFeedback_FeedbackId(feedbackId);
    }

    // ================= HELPERS =================

    private Account getCurrentSeller(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        String email = principal.getName();
        Long accId = accountRepo.findIdByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
        return accountRepo.findById(accId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
    }

    private static boolean isSpacedOutText(String s) {
        if (s == null) return false;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() < 9) return false;
        String[] parts = t.split(" ");
        if (parts.length < 5) return false;
        for (String p : parts) if (!p.matches("[\\p{L}\\p{N}]")) return false;
        return true;
    }
}
