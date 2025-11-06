package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    // Lấy danh sách token theo tool_id
    List<LicenseAccount> findByLicense_Tool_ToolId(Long toolId);

    // Lấy danh sách token theo tool_id
    List<LicenseAccount> findByTool_ToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);

    // Kiểm tra token đã tồn tại trong 1 tool chưa (chống trùng trong DB mode)
    boolean existsByToolAndToken(Tool tool, String token);
    boolean existsByToken(String token);
    @Transactional
    void deleteByTool(Tool tool);
    LicenseAccount findByToken(String token);
    List<LicenseAccount> findAllByTool(Tool tool);
    long countByToolToolIdAndLoginMethod(Long toolId, LicenseAccount.LoginMethod loginMethod);

    // Lấy tất cả license còn hoạt động nhưng đã hết hạn
    @Query("SELECT l FROM LicenseAccount l WHERE l.status = 'ACTIVE' AND l.endDate < :now")
    List<LicenseAccount> findExpiredAccounts(LocalDateTime now);

    Optional<LicenseAccount> findFirstByToolAndUsedFalse(Tool tool);

    Optional<LicenseAccount> findByOrder_OrderId(Long orderId);

    boolean existsByOrder_OrderId(Long orderId);
    List<LicenseAccount> findByToolToolId(Long toolId);
}

