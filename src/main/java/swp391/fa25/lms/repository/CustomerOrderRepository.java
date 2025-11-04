package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;

import java.util.List;

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
}
