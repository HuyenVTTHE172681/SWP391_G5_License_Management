package swp391.fa25.lms.repository;

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

    // THÊM MỚI: Tìm order bằng lastTxnRef cho callback retry
    @Query("SELECT o FROM CustomerOrder o WHERE o.lastTxnRef = :lastTxnRef")
    Optional<CustomerOrder> findByLastTxnRef(@Param("lastTxnRef") String lastTxnRef);
}
