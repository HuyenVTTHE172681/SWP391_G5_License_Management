package swp391.fa25.lms.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReply;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.*;


import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    public List<Feedback> findByTool(Tool tool);

    // Tính trung bình rating
    Long countByTool(Tool tool);
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Double findAverageRatingByTool(@Param("toolId") Long toolId);

    // Lấy feedback theo tool (dùng paging)
    Page<Feedback> findByTool(Tool tool, Pageable pageable);

    long countByTool_ToolIdAndAccount_AccountId(Long toolId, Long accountId);
    Optional<Feedback> findByFeedbackIdAndAccount_AccountId(Long feedbackId, Long accountId);

    @Modifying
    @Query("UPDATE Feedback f SET f.rating = :rating, f.comment = :comment " +
            "WHERE f.feedbackId = :fid AND f.account.accountId = :ownerId")
    int updateRatingAndCommentByIdAndOwner(@Param("fid") Long feedbackId,
                                           @Param("ownerId") Long ownerId,
                                           @Param("rating") Integer rating,
                                           @Param("comment") String comment);
    @Modifying
    @Query("DELETE FROM Feedback f WHERE f.feedbackId = :fid AND f.account.accountId = :ownerId")
    int deleteByIdAndOwner(@Param("fid") Long feedbackId, @Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Long countByToolId(Long toolId);
    List<Feedback> findByTool_Seller_AccountIdOrderByCreatedAtDesc(Long sellerId);
    List<Feedback> findByTool_Seller_AccountIdAndTool_ToolIdOrderByCreatedAtDesc(Long sellerId, Long toolId);

    @Query(value = "SELECT COALESCE(AVG(f.rating), 0) AS avg_rating " +
            "FROM feedback f " +
            "JOIN tool t ON f.tool_id = t.tool_id " +
            "WHERE t.seller_id = :sellerId " +
            "AND f.status = 'PUBLISHED'",
            nativeQuery = true)
    Double findAverageRatingBySellerId(@Param("sellerId") Long sellerId);

    // Lấy feedback theo tool + status
    Page<Feedback> findByToolAndStatus(Tool tool, Feedback.Status status, Pageable pageable);
}
