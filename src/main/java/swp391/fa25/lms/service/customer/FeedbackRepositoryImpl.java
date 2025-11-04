package swp391.fa25.lms.service.customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.FeedBackReplyRepository;
import swp391.fa25.lms.repository.FeedbackRepository;

import java.security.Principal;
import java.time.LocalDateTime;

@Service("customerFeedBack")
public class FeedbackRepositoryImpl {

    @Autowired
   private CustomerOrderRepository orderRepo;
    @Autowired
    private FeedbackRepository feedbackRepo;
    @Autowired
    private FeedBackReplyRepository feedbackReplyRepo;
    @Autowired
    private AccountRepository accountRepo;

    @PersistenceContext
    private EntityManager em;

    public FeedbackRepositoryImpl(CustomerOrderRepository orderRepo,
                                  FeedbackRepository feedbackRepo,
                                  FeedBackReplyRepository feedbackReplyRepo,
                                  AccountRepository accountRepo) {
        this.orderRepo = orderRepo;
        this.feedbackRepo = feedbackRepo;
        this.feedbackReplyRepo = feedbackReplyRepo;
        this.accountRepo = accountRepo;
    }

    // ====================== Nghiệp vụ public ======================

    @Transactional(readOnly = true)
    public CustomerOrder getOrderForFeedback(Long orderId) {
        var order = loadOrderOr404(orderId);
        ensureOrderSuccess(order);
        return order;
    }

    @Transactional
    public Long submitFeedback(Long orderId, Integer rating, String comment) {
        var order = loadOrderOr404(orderId);
        ensureOrderSuccess(order);

        String cmt = collapseSpaces(comment);
        if (looksSpacedOutText(cmt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung không hợp lệ: không chèn khoảng trắng giữa từng ký tự.");
        }

        var fb = new Feedback();
        fb.setAccount(order.getAccount());   // đúng chủ đơn (theo hành vi hiện tại)
        fb.setTool(order.getTool());
        fb.setRating(rating);
        fb.setComment(cmt);
        fb.setCreatedAt(LocalDateTime.now());

        em.persist(fb); // hoặc feedbackRepo.save(fb);
        return order.getTool().getToolId();
    }

    @Transactional(readOnly = true)
    public FeedbackEditView getFeedbackForEdit(Long feedbackId, Principal principal) {
        Long currentAccId = getCurrentAccountId(principal);

        // Tối ưu: lấy theo (id, owner)
        var owned = feedbackRepo.findByFeedbackIdAndAccount_AccountId(feedbackId, currentAccId);
        if (owned.isPresent()) {
            var fb = owned.get();
            String normalized = collapseSpaces(fb.getComment());
            return new FeedbackEditView(fb, normalized);
        }

        // Phân biệt 404/403
        var fb = em.find(Feedback.class, feedbackId);
        if (fb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");
    }

    @Transactional
    public Long updateFeedback(Long feedbackId, Integer rating, String comment, Principal principal) {
        Long currentAccId = getCurrentAccountId(principal);

        // Đọc để lấy toolId + phân biệt 404/403
        var fb = em.find(Feedback.class, feedbackId);
        if (fb == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback");
        if (!fb.getAccount().getAccountId().equals(currentAccId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");

        Long toolId = fb.getTool().getToolId();

        String cmt = collapseSpaces(comment);
        if (looksSpacedOutText(cmt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung không hợp lệ: không chèn khoảng trắng giữa từng ký tự.");
        }

        // Update 1 câu JPQL theo owner (tối ưu round-trip)
        int changed = feedbackRepo.updateRatingAndCommentByIdAndOwner(feedbackId, currentAccId, rating, cmt);
        if (changed != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể cập nhật feedback.");
        }
        return toolId;
    }

    @Transactional
    public Long deleteFeedback(Long feedbackId, Principal principal) {
        Long currentAccId = getCurrentAccountId(principal);

        var fb = em.find(Feedback.class, feedbackId);
        if (fb == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy feedback");
        if (!fb.getAccount().getAccountId().equals(currentAccId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền.");

        Long toolId = fb.getTool().getToolId();

        // Xoá reply trước, xoá feedback theo owner
        feedbackReplyRepo.deleteByFeedback_FeedbackId(fb.getFeedbackId());
        int changed = feedbackRepo.deleteByIdAndOwner(feedbackId, currentAccId);
        if (changed != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xoá feedback.");
        }
        return toolId;
    }

    /** DTO gọn cho view edit */
    public record FeedbackEditView(Feedback feedback, String normalizedComment) {}

    // ====================== Helpers ======================

    private CustomerOrder loadOrderOr404(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    private void ensureOrderSuccess(CustomerOrder order) {
        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn thành công mới được đánh giá.");
        }
    }

    private Long getCurrentAccountId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        String email = principal.getName();
        return accountRepo.findIdByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản."));
    }

    private static String collapseSpaces(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    /** phát hiện “c h ữ” */
    private static boolean looksSpacedOutText(String s) {
        if (s == null) return false;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() < 9) return false;
        String[] parts = t.split(" ");
        if (parts.length < 5) return false;
        for (String p : parts) if (!p.matches("[\\p{L}\\p{N}]")) return false;
        return true;
    }
}
