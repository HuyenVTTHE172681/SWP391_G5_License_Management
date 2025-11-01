package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;

import java.util.List;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    // Lấy danh sách token theo tool_id
    List<LicenseAccount> findByTool_ToolId(Long toolId);

    // Kiểm tra token đã tồn tại trong 1 tool chưa (chống trùng trong DB mode)
    boolean existsByToolAndToken(Tool tool, String token);
}



