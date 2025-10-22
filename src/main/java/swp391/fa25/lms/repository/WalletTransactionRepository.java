package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Wallet;
import swp391.fa25.lms.model.WalletTransaction;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    // Lấy tất cả giao dịch theo ví, mới nhất trước
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
}
