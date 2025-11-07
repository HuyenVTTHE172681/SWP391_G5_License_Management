package swp391.fa25.lms.util;

import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.repository.LicenseAccountRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LicenseAccountScheduler {
    private final LicenseAccountRepository licenseAccountRepository;

    public LicenseAccountScheduler(LicenseAccountRepository licenseAccountRepository) {
        this.licenseAccountRepository = licenseAccountRepository;
    }

    // Chạy mỗi 1 phút để check license hết hạn
    @Scheduled(fixedRate = 60000) // 60000 ms = 1 phút
    @Transactional
    public void updateExpiredLicenses() {
        LocalDateTime now = LocalDateTime.now();
        List<LicenseAccount> expiredAccounts = licenseAccountRepository.findExpiredAccounts(now);

        if (!expiredAccounts.isEmpty()) {
            for (LicenseAccount acc : expiredAccounts) {
                acc.setStatus(LicenseAccount.Status.EXPIRED);
                System.out.println("License expired: " + acc.getUsername());
            }
            licenseAccountRepository.saveAll(expiredAccounts);
        }
    }
}
