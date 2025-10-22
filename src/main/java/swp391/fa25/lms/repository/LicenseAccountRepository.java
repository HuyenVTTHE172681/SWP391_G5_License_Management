package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseAccount;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Integer> {
}
