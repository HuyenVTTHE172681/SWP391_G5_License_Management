package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.LicenseAccount;

import java.util.List;

public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    List<LicenseAccount> findByTool_ToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);
    boolean existsByToken(String token);
}

