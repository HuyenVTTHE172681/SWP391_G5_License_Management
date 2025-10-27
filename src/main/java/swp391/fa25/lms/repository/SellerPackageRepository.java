package swp391.fa25.lms.repository;

import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.SellerPackage;

@Registered
public interface SellerPackageRepository extends JpaRepository<SellerPackage, Integer> {
}
