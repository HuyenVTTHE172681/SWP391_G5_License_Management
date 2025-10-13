package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.License;

import java.util.List;

public interface LicenseRepo extends JpaRepository<License, Long> {
    List<License> findByToolToolId(Long toolId);
}
