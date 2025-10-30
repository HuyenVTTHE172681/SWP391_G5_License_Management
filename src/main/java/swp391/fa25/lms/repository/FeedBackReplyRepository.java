package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.FeedbackReply;

import java.util.List;
import java.util.Optional;

public interface FeedBackReplyRepository extends JpaRepository<FeedbackReply, Long> {

    Optional<FeedbackReply> findByFeedback_FeedbackId(Long feedbackId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional // <- dùng org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM FeedbackReply fr WHERE fr.feedback.feedbackId = :feedbackId")
    int deleteByFeedback_FeedbackId(Long feedbackId);
    List<FeedbackReply> findByFeedback_FeedbackIdIn(java.util.Collection<Long> feedbackIds);
}
