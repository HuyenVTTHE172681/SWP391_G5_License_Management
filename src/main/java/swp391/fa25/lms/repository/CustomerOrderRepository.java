package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder,Long> {
    List<CustomerOrder> findByToolSeller(Account seller);
    List<CustomerOrder> findByToolSellerAndPaymentStatus(Account seller, CustomerOrder.PaymentStatus status);


}
