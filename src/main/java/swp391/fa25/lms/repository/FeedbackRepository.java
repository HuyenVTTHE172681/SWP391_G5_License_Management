package swp391.fa25.lms.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByTool(Tool tool);

    // Tính trung bình rating (comment hơi sai, nhưng mình giữ nguyên cho bạn)
    Long countByTool(Tool tool);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Double findAverageRatingByTool(@Param("toolId") Long toolId);

    // Lấy feedback theo tool (dùng paging)
    Page<Feedback> findByTool(Tool tool, Pageable pageable);

    long countByTool_ToolIdAndAccount_AccountId(Long toolId, Long accountId);

    Optional<Feedback> findByFeedbackIdAndAccount_AccountId(Long feedbackId, Long accountId);

    @Modifying
    @Transactional
    @Query("""
            update Feedback f
            set f.rating = :rating,
                f.comment = :comment,
                f.status = :status
            where f.feedbackId = :fid
              and f.account.accountId = :ownerId
            """)
    int updateRatingCommentAndStatusByIdAndOwner(@Param("fid") Long feedbackId,
                                                 @Param("ownerId") Long ownerId,
                                                 @Param("rating") Integer rating,
                                                 @Param("comment") String comment,
                                                 @Param("status") Feedback.Status status);

    @Modifying
    @Transactional
    @Query("DELETE FROM Feedback f WHERE f.feedbackId = :fid AND f.account.accountId = :ownerId")
    int deleteByIdAndOwner(@Param("fid") Long feedbackId, @Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Long countByToolId(@Param("toolId") Long toolId);

    List<Feedback> findByTool_Seller_AccountIdOrderByCreatedAtDesc(Long sellerId);

    List<Feedback> findByTool_Seller_AccountIdAndTool_ToolIdOrderByCreatedAtDesc(Long sellerId, Long toolId);

    @Query(value = """
            SELECT COALESCE(AVG(f.rating), 0) AS avg_rating
            FROM feedback f
            JOIN tool t ON f.tool_id = t.tool_id
            WHERE t.seller_id = :sellerId
              AND f.status = 'PUBLISHED'
            """,
            nativeQuery = true)
    Double findAverageRatingBySellerId(@Param("sellerId") Long sellerId);

    Optional<Feedback> findByFeedbackIdAndAccount_Email(Long id, String email);

    @Query("""
           select avg(f.rating)
           from Feedback f
           where f.tool = :tool
             and (f.status = :status or f.status is null)
           """)
    Double avgRatingByToolAndStatusOrNull(@Param("tool") Tool tool,
                                          @Param("status") Feedback.Status status);

    @Query("""
           select count(f)
           from Feedback f
           where f.tool = :tool
             and (f.status = :status or f.status is null)
           """)
    long countByToolAndStatusOrNull(@Param("tool") Tool tool,
                                    @Param("status") Feedback.Status status);

    Page<Feedback> findByToolAndStatusOrderByCreatedAtDesc(Tool tool,
                                                           Feedback.Status status,
                                                           Pageable pageable);

    @Query("""
           select avg(f.rating)
           from Feedback f
           where f.tool = :tool
             and f.status = :status
           """)
    Double avgRatingByToolAndStatus(@Param("tool") Tool tool,
                                    @Param("status") Feedback.Status status);

    long countByToolAndStatus(Tool tool, Feedback.Status status);

    // Lấy feedback theo tool + status
    Page<Feedback> findByToolAndStatus(Tool tool, Feedback.Status status, Pageable pageable);

    long countByTool_Seller(Account seller);

    List<Feedback> findByTool_Seller(Account seller);

    List<Feedback> findByTool_ToolIdAndTool_Seller(Long toolId, Account seller);

    long countByTool_ToolIdAndTool_Seller(Long toolId, Account seller);

    long countByTool_ToolId(Long toolId);

    @Query("""
            SELECT f
            FROM Feedback f
            WHERE f.tool.seller.accountId = :sellerId
            """)
    List<Feedback> findAllBySellerId(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT f
            FROM Feedback f
            WHERE f.tool.seller.accountId = :sellerId
              AND f.tool.toolId = :toolId
            """)
    List<Feedback> findAllBySellerIdAndToolId(@Param("sellerId") Long sellerId,
                                              @Param("toolId") Long toolId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tool.toolId = :toolId AND f.status = 'PUBLISHED'")
    Long findAverageRatingByToolByStatus(@Param("toolId") Long toolId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.tool.toolId = :toolId AND f.status = 'PUBLISHED'")
    Long countByToolIdByStatus(@Param("toolId") Long toolId);

    @Query("""
           select f
           from Feedback f
           where f.tool = :tool
             and (f.status = :status or f.status is null)
           """)
    Page<Feedback> findByToolAndStatusOrNull(@Param("tool") Tool tool,
                                             @Param("status") Feedback.Status status,
                                             Pageable pageable);

    // ⚠️ CHÚ Ý: method này KHÔNG có @Query, để Spring Data tự sinh query dựa trên field "order" trong Feedback
    boolean existsByOrder(CustomerOrder order);
}
