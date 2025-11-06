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

    // Kiểm tra token đã tồn tại trong 1 tool chưa (chống trùng trong DB mode)
    boolean existsByLicense_Tool_ToolIdAndToken(Long toolId, String token);
    boolean existsByToken(String token);

    // Lấy tất cả license còn hoạt động nhưng đã hết hạn
    @Query("SELECT l FROM LicenseAccount l WHERE l.status = 'ACTIVE' AND l.endDate < :now")
    List<LicenseAccount> findExpiredAccounts(LocalDateTime now);

//    Optional<LicenseAccount> findFirstByToolAndUsedFalse(Tool tool);
    Optional<LicenseAccount> findFirstByLicense_ToolAndUsedFalse(Tool tool);

    Optional<LicenseAccount> findByOrder_OrderId(Long orderId);

    boolean existsByOrder_OrderId(Long orderId);
    @Transactional
    void deleteByLicense_Tool_ToolId(Long toolId);
    LicenseAccount findByToken(String token);
    
}

