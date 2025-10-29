package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.FeedbackReply;

import java.util.Optional;

public interface FeedBackReplyRepository extends JpaRepository<FeedbackReply, Long> {
    Optional<FeedbackReply> findByFeedback_FeedbackId(Long feedbackId);
    int deleteByFeedback_FeedbackId(Long feedbackId);
}
