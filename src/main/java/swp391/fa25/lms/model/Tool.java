package swp391.fa25.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Tool")
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long toolId;

    private String toolName;
    private String image;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Account seller;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status { PENDING, APPROVED, REJECTED }

    @OneToMany(mappedBy = "tool")
    private List<ToolFile> files;

    @OneToMany(mappedBy = "tool")
    private List<License> licenses;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
