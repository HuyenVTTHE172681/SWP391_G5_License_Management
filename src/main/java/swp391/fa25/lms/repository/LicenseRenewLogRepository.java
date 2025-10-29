package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.LicenseRenewLog;

public interface LicenseRenewLogRepository extends JpaRepository<LicenseRenewLog, Long>{
}
