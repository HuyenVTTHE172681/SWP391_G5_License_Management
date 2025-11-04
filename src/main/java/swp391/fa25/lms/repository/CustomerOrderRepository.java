package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByToolSeller(Account seller);
    List<CustomerOrder> findByToolSellerAndOrderStatus(Account seller, CustomerOrder.OrderStatus status);

    // Lấy danh sách order của 1 user, mới nhất trước
    List<CustomerOrder> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);
    /**
     * 💰 Doanh thu theo tháng
     */
    @Query("""
        SELECT MONTH(o.createdAt), SUM(o.price)
        FROM CustomerOrder o
        WHERE o.orderStatus = 'SUCCESS'
        GROUP BY MONTH(o.createdAt)
        ORDER BY MONTH(o.createdAt)
    """)
    List<Object[]> getMonthlyRevenue();

    /**
     * 🥇 Top 5 tool doanh thu cao nhất
     */
    @Query("""
        SELECT t.toolName, SUM(o.price)
        FROM CustomerOrder o
        JOIN o.license l
        JOIN l.tool t
        WHERE o.orderStatus = 'SUCCESS'
        GROUP BY t.toolName
        ORDER BY SUM(o.price) DESC
        LIMIT 5
    """)
    List<Object[]> getTop5ToolsByRevenue();

    /**
     * 🏆 Top 5 seller doanh thu cao nhất
     */
    @Query("""
        SELECT t.seller.fullName, SUM(o.price)
        FROM CustomerOrder o
        JOIN o.license l
        JOIN l.tool t
        WHERE o.orderStatus = 'SUCCESS'
        GROUP BY t.seller.fullName
        ORDER BY SUM(o.price) DESC
        LIMIT 5
    """)
    List<Object[]> getTop5SellersByRevenue();

    // THÊM MỚI: Tìm order bằng lastTxnRef cho callback retry
    @Query("SELECT o FROM CustomerOrder o WHERE o.lastTxnRef = :lastTxnRef")
    Optional<CustomerOrder> findByLastTxnRef(@Param("lastTxnRef") String lastTxnRef);
    List<CustomerOrder> findByTool_Seller_AccountId(Long sellerId);

    List<CustomerOrder> findByTool_ToolIdAndTool_Seller_AccountId(Long toolId, Long sellerId);
    @Query("""
           select o from CustomerOrder o
           join fetch o.tool t
           left join fetch o.licenseAccount la
           left join fetch o.license l
           where o.orderId = :id
           """)
    Optional<CustomerOrder> findWithToolAndLicenseAccountById(@Param("id") Long id);
    @EntityGraph(attributePaths = {"tool","licenseAccount","license"})
    Optional<CustomerOrder> findByOrderIdAndAccount_AccountId(Long orderId, Long accountId);
    @EntityGraph(attributePaths = {
            "tool",
            "license",
            "licenseAccount",
            "licenseAccount.renewAcc"   // <— load lịch sử gia hạn
    })
    Optional<CustomerOrder> findByOrderI
}
