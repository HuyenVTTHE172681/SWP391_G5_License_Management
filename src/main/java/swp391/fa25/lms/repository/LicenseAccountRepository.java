package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    List<LicenseAccount> findByTool_ToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);
    boolean existsByToken(String token);
    long countByToolToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);

    Optional<LicenseAccount> findFirstByToolAndUsedFalse(Tool tool);

    // Lấy tất cả license còn hoạt động nhưng đã hết hạn
    @Query("SELECT l FROM LicenseAccount l WHERE l.status = 'ACTIVE' AND l.endDate < :now")
    List<LicenseAccount> findExpiredAccounts(LocalDateTime now);
}

