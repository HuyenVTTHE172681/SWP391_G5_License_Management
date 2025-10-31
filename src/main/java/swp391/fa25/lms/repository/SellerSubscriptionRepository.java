package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;

import java.util.List;

public interface SellerSubscriptionRepository extends JpaRepository<SellerSubscription, Integer> {
    List<SellerSubscription> findByAccountOrderByStartDateDesc(Account account);
}
