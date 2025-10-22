package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseAccount;

import java.util.List;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    List<LicenseAccount> findByTool_ToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);
    boolean existsByToken(String token);
    long countByToolToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);
}

