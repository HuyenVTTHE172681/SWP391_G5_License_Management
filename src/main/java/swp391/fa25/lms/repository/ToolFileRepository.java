package swp391.fa25.lms.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import swp391.fa25.lms.model.ToolFile;

public interface ToolFileRepository extends JpaRepository<ToolFile, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM ToolFile tf WHERE tf.tool.toolId = :toolId")
    void deleteAllByToolToolId(Long toolId);
}
