package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Account.AccountStatus;
import swp391.fa25.lms.model.Role.RoleName;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // --- Tra cứu cơ bản ---
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<Account> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String email, String fullName, Pageable pageable);
    boolean existsByEmailAndVerifiedTrue(String email);
    Optional<Account> findByVerificationCode(String code);

    long countByRole_RoleName(RoleName roleName);

    Page<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Account> findByStatusOrderByUpdatedAtDesc(AccountStatus status, Pageable pageable);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.accountId = :id")
    int updateStatus(@Param("id") long id, @Param("status") AccountStatus status);

    // Search có filter status
    @Query("""
           SELECT a FROM Account a
           WHERE (:q IS NULL OR :q = '' 
                 OR LOWER(a.email) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:status IS NULL OR a.status = :status)
           """)
    Page<Account> search(@Param("q") String q,
                         @Param("status") AccountStatus status,
                         Pageable pageable);
    Optional<Long> findIdByEmail(String email);


    List<Account> findTop8ByStatusOrderByUpdatedAtDesc(AccountStatus status);

}
