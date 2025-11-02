package swp391.fa25.lms.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import swp391.fa25.lms.model.Tool;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolFile;

import java.util.List;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToolFileRepository extends JpaRepository<ToolFile, Long> {
    List<ToolFile> findByTool(Tool tool);

    @Modifying
    @Transactional
    @Query("DELETE FROM ToolFile tf WHERE tf.tool.toolId = :toolId")
    void deleteAllByToolToolId(Long toolId);
    Optional<ToolFile> findTopByTool_ToolIdAndFileTypeOrderByCreatedAtDesc(
            Long toolId, ToolFile.FileType fileType
    );

}
