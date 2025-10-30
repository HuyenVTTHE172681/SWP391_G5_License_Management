package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.fa25.lms.model.LicenseRenewLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LicenseRenewLogRepository extends JpaRepository<LicenseRenewLog, Long>{
    List<LicenseRenewLog> findByLicenseAccount_LicenseAccountIdOrderByRenewDateDesc(Long licenseAccountId);
    @Query("""
        select r from LicenseRenewLog r
        where r.licenseAccount.licenseAccountId = :accId
          and (:from is null or r.renewDate >= :from)
          and (:to   is null or r.renewDate <= :to)
          and (:min  is null or coalesce(r.amountPaid, 0) >= :min)
          and (:max  is null or coalesce(r.amountPaid, 0) <= :max)
        """)
    Page<LicenseRenewLog> search(
                    @Param("accId") Long accId,
                    @Param("from") LocalDateTime from,
                    @Param("to")   LocalDateTime to,
                    @Param("min") BigDecimal min,
                    @Param("max")  BigDecimal max,
                    Pageable pageable
            );
}
