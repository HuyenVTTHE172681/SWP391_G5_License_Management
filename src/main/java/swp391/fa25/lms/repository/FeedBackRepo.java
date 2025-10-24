package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import java.util.Optional;

@Repository
public interface FeedBackRepo extends JpaRepository<Feedback, Long> {
    // Tinh AVG rating
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Double findAverageRatingByTool(@Param("toolId") Long toolId);
    Long countByTool(Tool tool);
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
}
