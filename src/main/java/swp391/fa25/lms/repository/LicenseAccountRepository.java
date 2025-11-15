package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    // Lấy danh sách token theo tool_id
    List<LicenseAccount> findByLicense_Tool_ToolId(Long toolId);

    Optional<LicenseAccount> findByUsername(String username);


    // Kiểm tra token đã tồn tại trong 1 tool chưa (chống trùng trong DB mode)
    boolean existsByLicense_Tool_ToolIdAndToken(Long toolId, String token);

    boolean existsByToken(String token);

    // Lấy tất cả license còn hoạt động nhưng đã hết hạn
    @Query("SELECT l FROM LicenseAccount l WHERE l.status = 'ACTIVE' AND l.endDate < :now")
    List<LicenseAccount> findExpiredAccounts(LocalDateTime now);

    //    Optional<LicenseAccount> findFirstByToolAndUsedFalse(Tool tool);
    Optional<LicenseAccount> findFirstByLicense_ToolAndUsedFalse(Tool tool);

    // Custom query cho TOKEN unused (join explicit, order by ID để deterministic)
    // FIX: Native query SQL Server compatible (TOP 1 thay LIMIT 1 để lấy unique first row)
    @Query(value = "SELECT TOP 1 la.* FROM License_Account la " +
            "JOIN License l ON la.license_id = l.license_id " +
            "JOIN Tool t ON l.tool_id = t.tool_id " +
            "WHERE t.tool_id = :toolId AND la.used = 0 " +
            "ORDER BY la.license_account_id ASC",
            nativeQuery = true)
    Optional<LicenseAccount> findFirstByLicense_Tool_ToolIdAndUsedFalse(@Param("toolId") Long toolId);

    Optional<LicenseAccount> findByOrder_OrderId(Long orderId);

    boolean existsByOrder_OrderId(Long orderId);

    @Transactional
    void deleteByLicense_Tool_ToolId(Long toolId);

    LicenseAccount findByToken(String token);
    Optional<LicenseAccount> findByOrder(CustomerOrder order);

    Optional<LicenseAccount> findByTokenAndLicense_Tool_ToolId(String token, Long toolId);

    Optional<LicenseAccount> findByUsernameAndPasswordAndLicense_Tool_ToolId(
            String username,
            String password,
            Long toolId
    );

    List<LicenseAccount> findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status status, Long licenseToolToolId);
}

