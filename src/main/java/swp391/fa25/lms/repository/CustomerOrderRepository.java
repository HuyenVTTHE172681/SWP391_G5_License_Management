package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;

import java.util.List;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    // Lấy danh sách order của 1 user, mới nhất trước
    List<CustomerOrder> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);
}
