package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Account.AccountStatus;
import swp391.fa25.lms.model.Role.RoleName;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // --- Tra cứu cơ bản ---
    Optional<Account> findByEmail(String email);
    Optional<Account> findByVerificationToken(String token);
    boolean existsByEmail(String email);

    // Tìm kiếm theo keyword (email hoặc fullName)
    Page<Account> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String email, String fullName, Pageable pageable);

    // --- Dùng cho Tab Home (thống kê + danh sách) ---
    long countByRole_RoleName(RoleName roleName);

    // Đăng ký mới nhất (sắp xếp theo createdAt giảm dần)
    Page<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Danh sách DEACTIVATED mới cập nhật gần đây
    Page<Account> findByStatusOrderByUpdatedAtDesc(AccountStatus status, Pageable pageable);
}
