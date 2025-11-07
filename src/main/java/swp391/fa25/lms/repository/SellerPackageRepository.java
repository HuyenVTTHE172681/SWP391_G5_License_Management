package swp391.fa25.lms.repository;

import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import swp391.fa25.lms.model.SellerPackage;

import java.util.List;

@Registered
public interface SellerPackageRepository extends JpaRepository<SellerPackage, Integer>, JpaSpecificationExecutor<SellerPackage> {
    List<SellerPackage> findByPackageNameContainingIgnoreCase(String name);
    List<SellerPackage> findByStatus(SellerPackage.Status status);
    List<SellerPackage> findByPackageNameContainingIgnoreCaseAndStatus(String name, SellerPackage.Status status);
    boolean existsByPackageName(String name);
}
