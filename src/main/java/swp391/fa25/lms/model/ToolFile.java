package swp391.fa25.lms.model;

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
}
