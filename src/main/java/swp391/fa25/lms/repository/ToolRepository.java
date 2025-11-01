package swp391.fa25.lms.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {

    List<Tool> findAllByToolNameContainingIgnoreCaseAndCategory_CategoryId(
            String toolName, Long categoryId, Sort sort);

    List<Tool> findAllByToolNameContainingIgnoreCase(String toolName, Sort sort);

    List<Tool> findAll(Sort sort);

    List<Tool> findAll(Specification<Tool> spec);

    Tool findByToolId(long toolId);

    List<Tool> findByStatus(Tool.Status status);

    @Query("""
        SELECT t FROM Tool t
        WHERE (:toolName IS NULL OR LOWER(t.toolName) LIKE LOWER(CONCAT('%', :toolName, '%')))
          AND (:categoryId IS NULL OR t.category.categoryId = :categoryId)
          AND (:status IS NULL OR t.status = :status)
        ORDER BY t.updatedAt DESC
    """)
    List<Tool> filterToolsForModerator(@Param("toolName") String toolName,
                                       @Param("categoryId") Long categoryId,
                                       @Param("status") Tool.Status status);

    List<Tool> findByToolNameContainingIgnoreCase(String keyword);

    List<Tool> findByStatusNot(Tool.Status status);

    // Lấy Tool theo id status PUBLISHED
    @EntityGraph(attributePaths = {"licenses", "seller", "category"})
    Optional<Tool> findByToolIdAndStatus(Long toolId, Tool.Status status);

    Optional<Tool> findById(Long toolId);

    @Query("""
    SELECT DISTINCT t FROM Tool t
    LEFT JOIN t.licenses l
    WHERE t.seller.accountId = :sellerId
      AND (:keyword IS NULL OR LOWER(t.toolName) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:categoryId IS NULL OR t.category.categoryId = :categoryId)
      AND (:status IS NULL OR t.status = :status)
      AND (:loginMethod IS NULL OR t.loginMethod = :loginMethod)
      AND (:minPrice IS NULL OR l.price >= :minPrice)
      AND (:maxPrice IS NULL OR l.price <= :maxPrice)
""")
    Page<Tool> searchToolsForSeller(
            @Param("sellerId") Long sellerId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("loginMethod") Tool.LoginMethod loginMethod,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );
    List<Tool> findBySellerAndStatusNot(Account seller, Tool.Status status);
    boolean existsByToolName(String toolName);
    }
