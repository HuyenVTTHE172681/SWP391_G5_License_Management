package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "Tool_File")
public class ToolFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    @JsonBackReference(value = "tool-files")
    private Tool tool;

    @NotBlank(message = "File path cannot be blank")
    @Column(nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    public enum FileType {
        ORIGINAL, WRAPPED
    }

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private Account uploadedBy;

    private LocalDateTime createdAt;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public Account getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Account uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
