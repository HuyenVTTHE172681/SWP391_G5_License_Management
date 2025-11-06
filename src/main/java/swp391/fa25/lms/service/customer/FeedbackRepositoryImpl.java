package swp391.fa25.lms.service.customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
@Transactional
public class FeedbackRepositoryImpl {

    @Autowired
    private CustomerOrderRepository orderRepo;

    @Autowired
    private FeedbackRepository feedbackRepo;

    @Autowired
    private FeedBackReplyRepository feedbackReplyRepo; // hiện chưa dùng, để lại nếu cần

    @Autowired
    private AccountRepository accountRepo;            // hiện chưa dùng, để lại nếu cần

    @PersistenceContext
    private EntityManager em;

    /** DTO gọn cho view edit */
    public record FeedbackEditView(Feedback feedback, String normalizedComment) {}

    // ====================== Nghiệp vụ public ======================

    @Transactional(readOnly = true)
    public CustomerOrder getOrderForFeedback(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    /** Tạo feedback mới => PUBLISHED (soft-state) */
    public Long submitFeedback(Long orderId, Integer rating, String comment) {
        var order = getOrderForFeedback(orderId);

        Feedback fb = new Feedback();
        fb.setAccount(order.getAccount());
        fb.setTool(order.getTool());
        fb.setRating(rating);
        fb.setComment((comment == null || comment.isBlank()) ? null : comment.trim());
        fb.setStatus(Feedback.Status.PUBLISHED);   // <== quan trọng
        fb.setCreatedAt(LocalDateTime.now());

        feedbackRepo.save(fb);
        return order.getTool().getToolId();
    }

    /** Lấy form sửa (ràng buộc chủ sở hữu theo email đăng nhập) */
    @Transactional(readOnly = true)
    public FeedbackEditView getFeedbackForEdit(Long feedbackId, Principal principal) {
        var fb = feedbackRepo.findByFeedbackIdAndAccount_Email(feedbackId, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Không có quyền"));

        String normalized = (fb.getComment() == null) ? "" : fb.getComment().trim();
        return new FeedbackEditView(fb, normalized);
    }

    /** Cập nhật feedback (giữ/chuẩn hoá trạng thái) */
    public Long updateFeedback(Long feedbackId,
                               Integer rating,
                               String comment,
                               Feedback.Status status,
                               Principal principal) {

        var fb = feedbackRepo.findByFeedbackIdAndAccount_Email(feedbackId, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Không có quyền"));

        fb.setRating(rating);
        fb.setComment((comment == null || comment.isBlank()) ? null : comment.trim());

        if (status != null) {
            // Rule nghiệp vụ: không cho user set SUSPECT bằng form
            if (status == Feedback.Status.SUSPECT) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không thể đặt trạng thái SUSPECT trong form sửa. Vui lòng dùng chức năng Xoá."
                );
            }
            // Cho phép PUBLISHED hoặc HIDDEN
            fb.setStatus(status);
        } else if (fb.getStatus() == null) {
            fb.setStatus(Feedback.Status.PUBLISHED);
        }

        feedbackRepo.save(fb);
        return fb.getTool().getToolId();
    }

    /** “Xoá” => chuyển sang SUSPECT (soft-delete), KHÔNG xoá DB */
    public Long deleteFeedback(Long feedbackId, Principal principal) {
        var fb = feedbackRepo.findByFeedbackIdAndAccount_Email(feedbackId, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Không có quyền"));

        fb.setStatus(Feedback.Status.SUSPECT);     // <== soft-delete
        feedbackRepo.save(fb);
        return fb.getTool().getToolId();
    }

    // ====================== Helpers (nếu cần dùng sau) ======================
}
