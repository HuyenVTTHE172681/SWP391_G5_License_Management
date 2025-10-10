package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Account;

import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account, Long> {
    long countByStatus(Account.AccountStatus status);
    boolean existsByEmail(String email);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByVerificationToken(String token);
}
