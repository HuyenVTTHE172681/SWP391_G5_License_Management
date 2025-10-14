package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.License;

import java.util.List;

@Repository
public interface LicenseToolRepository extends JpaRepository<License, Long>{
    List<License> findByToolToolId(Long toolId);
}
